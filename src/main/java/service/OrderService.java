package service;

import dao.FoodItemDAO;
import dao.OrderDAO;
import dao.RestaurantDAO;
import dao.UserDAO;
import dto.OrderDto;
import entity.*;
import lombok.AllArgsConstructor;
import service.exception.OrderServiceExceptions;
import service.exception.RestaurantServiceExceptions;
import service.exception.UserNotFoundException;

import java.math.BigDecimal;
import java.util.ArrayList;


@AllArgsConstructor
public class OrderService {

    private final UserDAO userDAO;
    private final RestaurantDAO restaurantDAO;
    private final FoodItemDAO foodItemDAO;
    private final OrderDAO orderDAO;


    public OrderDto.OrderResponse createOrder(OrderDto.CreateRequest requestDto, String customerUserPhone) throws
            OrderServiceExceptions.UserNotBuyerException,
            OrderServiceExceptions.ItemOutOfStockException,
            UserNotFoundException, RestaurantServiceExceptions.RestaurantNotFound,
            IllegalArgumentException, RestaurantServiceExceptions.ItemNotFound {
        User customer = userDAO.findByPhone(customerUserPhone).
                orElseThrow(() -> new UserNotFoundException("Customer not found"));

        if (!customer.getRole().equals(Role.BUYER)) {
            throw new OrderServiceExceptions.UserNotBuyerException("This user is not a buyer");
        }

        Restaurant restaurant = restaurantDAO.findRestaurantById(requestDto.getVendor_id())
                .orElseThrow(() -> new RestaurantServiceExceptions.RestaurantNotFound("Restaurant not found"));

        // TODO: Add checking for coupon id validation later


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
                throw new OrderServiceExceptions.ItemOutOfStockException("Food " + food.getName() + " supply is not enough");


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

        orderDAO.save(order);


        // updating the supply of each food item
        for (FoodItem foodItem : foodItemsToUpdate) {
            foodItemDAO.update(foodItem);
        }

        return mapOrderToResponseDto(order);
    }

    public ArrayList<OrderDto.OrderResponse> getOrderHistory(String customerUserPhone, String vendorName, String foodName) throws
            UserNotFoundException, OrderServiceExceptions.UserNotBuyerException {
        User customer = userDAO.findByPhone(customerUserPhone).
                orElseThrow(() -> new UserNotFoundException("Customer not found"));

        if (!customer.getRole().equals(Role.BUYER)) {
            throw new OrderServiceExceptions.UserNotBuyerException("This user is not a buyer");
        }

        ArrayList<OrderDto.OrderResponse> response = new ArrayList<>();
        for (Order order : orderDAO.findHistoryByCustomer(customer.getId(), vendorName, foodName)){
            response.add(mapOrderToResponseDto(order));
        }
        return response;
    }

    private OrderDto.OrderResponse mapOrderToResponseDto(Order order) {
        OrderDto.OrderResponse response = new OrderDto.OrderResponse();
        response.setId(order.getId());
        response.setDelivery_address(order.getDeliveryAddress());
        response.setCustomer_id(order.getCustomer().getId());
        response.setVendor_id(order.getRestaurant().getId());
        // TODO: set the coupon id
        response.setRaw_price(order.getRawPrice());
        response.setTax_fee(order.getTaxFee());
        response.setAdditional_fee(order.getAdditionalFee());
        // TODO: set the courier fee
        response.setPay_price(order.getTotalPrice());
        response.setStatus(order.getStatus().name());
        response.setCreated_at(order.getCreatedAt().toString());
        response.setUpdated_at(order.getUpdatedAt().toString());


        ArrayList<Long> itemIds = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            itemIds.add(item.getFoodItem().getId());
        }
        response.setItem_ids(itemIds);

        return response;
    }
}
