package service;

import dao.*;
import dto.MessageDto;
import dto.OrderDto;
import entity.*;
import lombok.AllArgsConstructor;
import org.hibernate.Session;
import org.hibernate.Transaction;
import service.exception.*;
import util.HibernateUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;


@AllArgsConstructor
public class OrderService {

    private final UserDAO userDAO;
    private final RestaurantDAO restaurantDAO;
    private final FoodItemDAO foodItemDAO;
    private final OrderDAO orderDAO;
    private final CouponDAO couponDAO;


    public OrderDto.OrderResponse createOrder(OrderDto.CreateRequest requestDto, String customerUserPhone) throws
            OrderServiceExceptions.UserNotBuyer,
            OrderServiceExceptions.ItemOutOfStock,
            UserNotFoundException, RestaurantServiceExceptions.RestaurantNotFound,
            IllegalArgumentException, RestaurantServiceExceptions.ItemNotFound,
            CouponServiceExceptions.CouponNotFound, UserNotApprovedException,
            CouponServiceExceptions.InvalidCoupon {

        User customer = userDAO.findByPhone(customerUserPhone).
                orElseThrow(() -> new UserNotFoundException("Buyer not found"));

        if (!customer.getRole().equals(Role.BUYER)) {
            throw new OrderServiceExceptions.UserNotBuyer("This user is not a buyer");
        }

        if (!customer.getApprovalStatus().equals(ApprovalStatus.APPROVED)) {
            throw new UserNotApprovedException("This is Buyer is not approved");
        }

        Restaurant restaurant = restaurantDAO.findRestaurantById(requestDto.getVendor_id())
                .orElseThrow(() -> new RestaurantServiceExceptions.RestaurantNotFound("Restaurant not found"));

        Coupon coupon = null;

        if (requestDto.getCoupon_id() != null) {
            coupon = couponDAO.findCouponById(requestDto.getCoupon_id()).orElseThrow(
                    () -> new CouponServiceExceptions.CouponNotFound("Coupon Not Found")
            );
        }

        if (coupon != null) {
            if (LocalDate.now().isAfter(coupon.getEndDate())){
                throw new CouponServiceExceptions.InvalidCoupon("This coupon has expired");
            }
            if (LocalDate.now().isBefore(coupon.getStartDate())){
                throw new CouponServiceExceptions.InvalidCoupon("This coupon has not been activated");
            }
            if (coupon.getUserCount() <= 0){
                throw new CouponServiceExceptions.InvalidCoupon("This coupon has no uses left");
            }
        }

        String address = requestDto.getDelivery_address();
        if (address == null || address.isBlank())
            throw new IllegalArgumentException("Invalid Delivery Address");

        if (requestDto.getItems() == null || requestDto.getItems().isEmpty())
            throw new IllegalArgumentException("Items list is empty");


        ArrayList<FoodItem> foodItemsToUpdate = new ArrayList<>();
        ArrayList<OrderItem> orderFoodItems = new ArrayList<>();


        BigDecimal rawPrice = BigDecimal.ZERO;

        for (OrderDto.OrderItemRequest requestItem : requestDto.getItems()) {
            FoodItem food = foodItemDAO.findFoodItemById(restaurant.getId(), requestItem.getItem_id())
                    .orElseThrow(() -> new RestaurantServiceExceptions.ItemNotFound("Food with ID" + requestItem.getItem_id() + " not found in this restaurant"));


            if (requestItem.getQuantity() > food.getSupply())
                throw new OrderServiceExceptions.ItemOutOfStock("Food " + food.getName() + " supply is not enough");


            BigDecimal thisItemTotalPrice = new BigDecimal(food.getPrice()).multiply(new BigDecimal(requestItem.getQuantity()));
            rawPrice = rawPrice.add(thisItemTotalPrice);

            OrderItem orderItem = new OrderItem();
            orderItem.setFoodItem(food);
            orderItem.setQuantity(requestItem.getQuantity());
            orderItem.setPriceAtOrderTime(BigDecimal.valueOf(food.getPrice()));
            orderFoodItems.add(orderItem);

            food.setSupply(food.getSupply() - requestItem.getQuantity());
            foodItemsToUpdate.add(food);

        }

        BigDecimal taxFee = BigDecimal.valueOf(restaurant.getTaxFee());
        BigDecimal additionalFee = BigDecimal.valueOf(restaurant.getAdditionalFee());
        BigDecimal totalPrice = rawPrice.add(taxFee).add(additionalFee);




        Order order = new Order();


        order.setCustomer(customer);
        order.setRestaurant(restaurant);
        order.setDeliveryAddress(address);
        order.setRawPrice(rawPrice);
        order.setTaxFee(taxFee);
        order.setAdditionalFee(additionalFee);
        order.setTotalPrice(totalPrice);
        order.setStatus(OrderStatus.SUBMITTED);


        // setting the connection between all order items and the respective order it belongs to
        for (OrderItem item : orderFoodItems) {
            item.setOrder(order);
            order.getItems().add(item);
        }

        Transaction transaction = null;
        Session session = null;


        try {

            session = HibernateUtil.getSessionFactory().openSession();
            transaction = session.beginTransaction();

            if (coupon != null) {

                if (rawPrice.compareTo(coupon.getMinPrice()) < 0) {
                    throw new CouponServiceExceptions.InvalidCoupon("Order price is lower than min coupon price");
                }

                if (coupon.getCouponType().equals(CouponType.FIXED)) {
                    totalPrice = totalPrice.subtract(coupon.getCouponValue());
                }

                if (coupon.getCouponType().equals(CouponType.PERCENT)) {
                    BigDecimal discountAmount = rawPrice.multiply(coupon.getCouponValue().divide(new BigDecimal("100")));
                    totalPrice = totalPrice.subtract(discountAmount);
                }

                order.setTotalPrice(totalPrice);

                order.setCoupon(coupon);
                coupon.setUserCount(coupon.getUserCount() - 1);
                couponDAO.update(session, coupon);

            }

            orderDAO.save(session, order);



            // updating the supply of each food item
            for (FoodItem foodItem : foodItemsToUpdate) {
                foodItemDAO.update(session, foodItem);
            }


            transaction.commit();

        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException("Error while saving order: " + e.getMessage(), e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }

        return mapOrderToResponseDto(order);
    }

    public ArrayList<OrderDto.OrderResponse> getOrderHistory(String customerUserPhone, String vendorName, String foodName) throws
            UserNotFoundException, OrderServiceExceptions.UserNotBuyer {
        User customer = userDAO.findByPhone(customerUserPhone).
                orElseThrow(() -> new UserNotFoundException("Buyer not found"));

        if (!customer.getRole().equals(Role.BUYER)) {
            throw new OrderServiceExceptions.UserNotBuyer("This user is not a buyer");
        }

        if (!customer.getApprovalStatus().equals(ApprovalStatus.APPROVED)) {
            throw new UserNotApprovedException("This is Buyer is not approved");
        }

        ArrayList<OrderDto.OrderResponse> response = new ArrayList<>();
        for (Order order : orderDAO.findHistoryByCustomer(customer.getId(), vendorName, foodName)) {
            response.add(mapOrderToResponseDto(order));
        }
        return response;
    }

    public OrderDto.OrderResponse getOrderById(String customerUserPhone, Long orderId) throws
            UserNotFoundException, OrderServiceExceptions.OrderNotFound,
            OrderServiceExceptions.NotOwnerOfOrder, OrderServiceExceptions.UserNotBuyer {

        User customer = userDAO.findByPhone(customerUserPhone).
                orElseThrow(() -> new UserNotFoundException("Buyer not found"));

        if (!customer.getRole().equals(Role.BUYER)) {
            throw new OrderServiceExceptions.UserNotBuyer("This user is not a buyer");
        }

        if (!customer.getApprovalStatus().equals(ApprovalStatus.APPROVED)) {
            throw new UserNotApprovedException("This is Buyer is not approved");
        }

        Order order = orderDAO.findOrderById(orderId).
                orElseThrow(() -> new OrderServiceExceptions.OrderNotFound
                        ("Order with ID" + orderId + " not found"));

        if (!order.getCustomer().equals(customer)) {
            throw new OrderServiceExceptions.NotOwnerOfOrder("You are not the owner of this order");
        }

        return mapOrderToResponseDto(order);
    }

    public ArrayList<OrderDto.OrderResponse> getRestaurantOrders(String ownerUserPhone, Long restaurantId, String status, String search, String user, String courier) throws
            UserNotFoundException, RestaurantServiceExceptions.UserNotSeller,
            RestaurantServiceExceptions.RestaurantNotFound, RestaurantServiceExceptions.UserNotOwner {

        User owner = userDAO.findByPhone(ownerUserPhone).
                orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!owner.getRole().equals(Role.SELLER)) {
            throw new RestaurantServiceExceptions.UserNotSeller("This user is not a seller");
        }

        if (!owner.getApprovalStatus().equals(ApprovalStatus.APPROVED)) {
            throw new UserNotFoundException("This seller is not approved");
        }

        Restaurant restaurant = restaurantDAO.findRestaurantById(restaurantId).orElseThrow(
                () -> new RestaurantServiceExceptions.RestaurantNotFound("Restaurant with ID" + restaurantId + " not found"));

        if (!restaurant.getOwner().equals(owner)) {
            throw new RestaurantServiceExceptions.UserNotOwner("You are not the owner of this restaurant");
        }

        ArrayList<OrderDto.OrderResponse> response = new ArrayList<>();

        for (Order order : orderDAO.findByRestaurantId(restaurantId, status, search, user, courier)) {
            response.add(mapOrderToResponseDto(order));
        }

        return response;
    }

    public MessageDto updateOrderStatus(OrderDto.OrderStatusChangeRequest requestDto, String ownerUserPhone, Long orderId) throws
            UserNotFoundException, RestaurantServiceExceptions.RestaurantNotFound,
            OrderServiceExceptions.OrderNotFound,
            OrderServiceExceptions.RestaurantNotOwnerOfOrder, IllegalArgumentException {

        User owner = userDAO.findByPhone(ownerUserPhone).orElseThrow(
                () -> new UserNotFoundException("User not found"));

        if (!owner.getRole().equals(Role.SELLER)) {
            throw new RestaurantServiceExceptions.UserNotSeller("This user is not a seller");
        }

        if (!owner.getApprovalStatus().equals(ApprovalStatus.APPROVED)) {
            throw new UserNotApprovedException("This seller is not approved");
        }

        Restaurant restaurant = restaurantDAO.findRestaurantByOwner(owner).orElseThrow(
                () -> new RestaurantServiceExceptions.RestaurantNotFound("You have no restaurant"));

        Order order = orderDAO.findOrderById(orderId).orElseThrow(
                () -> new OrderServiceExceptions.OrderNotFound("Order with ID" + orderId + " not found"));

        if (!order.getRestaurant().equals(restaurant)) {
            throw new OrderServiceExceptions.RestaurantNotOwnerOfOrder(
                    "This restaurant is not the owner of this order");
        }

        try {
            order.setStatus(OrderStatus.valueOf(requestDto.getStatus().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid order status");
        }

        orderDAO.updateOrder(order);
        return new MessageDto("Order status changed successfully");
    }

    private OrderDto.OrderResponse mapOrderToResponseDto(Order order) {
        OrderDto.OrderResponse response = new OrderDto.OrderResponse();
        response.setId(order.getId());
        response.setDelivery_address(order.getDeliveryAddress());
        response.setCustomer_id(order.getCustomer().getId());
        response.setVendor_id(order.getRestaurant().getId());
        if (order.getCoupon() != null) {
            response.setCoupon_id(order.getCoupon().getId());
        }
        response.setRaw_price(order.getRawPrice());
        response.setTax_fee(order.getTaxFee());
        response.setAdditional_fee(order.getAdditionalFee());
        response.setCourier_fee(order.getCourierFee());
        response.setPay_price(order.getTotalPrice());
        response.setStatus(order.getStatus().name());
        response.setCreated_at(order.getCreatedAt().toString());
        response.setUpdated_at(order.getUpdatedAt().toString());

        if (order.getCourier() != null) {
            response.setCourier_id(order.getCourier().getId());
        }


        ArrayList<Long> itemIds = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            itemIds.add(item.getFoodItem().getId());
        }
        response.setItem_ids(itemIds);

        return response;
    }

}
