package service;

import dao.OrderDAO;
import dao.UserDAO;
import dto.DeliverDto;
import dto.OrderDto;
import entity.*;
import service.exception.DeliveryServiceExceptions;
import service.exception.OrderServiceExceptions;
import service.exception.UserNotApprovedException;
import service.exception.UserNotFoundException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

public class DeliveryService {

    private final BigDecimal COURIER_FEE = new BigDecimal("20000");
    private final UserDAO userDAO;
    private final OrderDAO orderDAO;

    public DeliveryService(UserDAO userDAO,OrderDAO orderDAO) {
        this.userDAO = userDAO;
        this.orderDAO = orderDAO;
    }

    public ArrayList<OrderDto.OrderResponse> getAvailableOrders(String courierPhoneNumber) throws
            UserNotFoundException, DeliveryServiceExceptions.UserNotCourier {

        User courier = userDAO.findByPhone(courierPhoneNumber).orElseThrow(
                () -> new UserNotFoundException("Courier not found")
        );

        if (!courier.getRole().equals(Role.COURIER)){
            throw new DeliveryServiceExceptions.UserNotCourier("This user is not a courier");
        }

        if (!courier.getApprovalStatus().equals(ApprovalStatus.APPROVED)){
            throw new UserNotApprovedException("This courier is not approved");
        }

        ArrayList<OrderDto.OrderResponse> orders = new ArrayList<>();

        for (Order order : orderDAO.findOrdersAwaitingDelivery()){
            orders.add(mapOrderToResponseDto(order));
        }

        return orders;
    }

    public DeliverDto.UpdateStatusResponse updateOrderStatus(OrderDto.OrderStatusChangeRequest requestDto, String courierPhoneNumber, Long orderId) throws
            UserNotFoundException, DeliveryServiceExceptions.UserNotCourier,
            OrderServiceExceptions.OrderNotFound, DeliveryServiceExceptions.OrderNotReadyForDelivery,
            DeliveryServiceExceptions.OrderAlreadyAssignedToCourier, DeliveryServiceExceptions.CourierIsBusy,
            IllegalArgumentException{

        User courier = userDAO.findByPhone(courierPhoneNumber).orElseThrow(
                () -> new UserNotFoundException("User not found")
        );

        if (!courier.getRole().equals(Role.COURIER)){
            throw new DeliveryServiceExceptions.UserNotCourier("This user is not a courier");
        }

        if (!courier.getApprovalStatus().equals(ApprovalStatus.APPROVED)){
            throw new UserNotApprovedException("This courier is not approved");
        }

        Order order = orderDAO.findOrderById(orderId).orElseThrow(
                () -> new OrderServiceExceptions.OrderNotFound("Order with ID" + orderId + " not found")
        );

        if (order.getCourier() != null && !order.getCourier().equals(courier)){
            throw new DeliveryServiceExceptions.OrderAlreadyAssignedToCourier("This order is already assigned to another courier");
        }

        Optional<Order> conflictingOrder = orderDAO.findActiveOrderByCourierId(courier.getId());

        if (conflictingOrder.isPresent()){
            if (!conflictingOrder.get().getId().equals(order.getId())){
                throw new DeliveryServiceExceptions.CourierIsBusy("This courier is already busy delivering order with ID" + orderId);
            }
        }

        try {
            order.setStatus(OrderStatus.valueOf(requestDto.getStatus().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid order status");
        }

        if (order.getCourier() == null){
            order.setCourier(courier);
//            order.setCourierFee(COURIER_FEE);
//            order.setTotalPrice(order.getTotalPrice().add(COURIER_FEE));
        }

        orderDAO.updateOrder(order);

        OrderDto.OrderResponse orderResponse = mapOrderToResponseDto(order);

        return new DeliverDto.UpdateStatusResponse("Changed status successfully", orderResponse);
    }


    public ArrayList<OrderDto.OrderResponse> getDeliveryHistory(String courierPhoneNumber, String search, String vendor, String user, String status) throws
            UserNotFoundException, DeliveryServiceExceptions.UserNotCourier {

        User courier = userDAO.findByPhone(courierPhoneNumber).orElseThrow(
                () -> new UserNotFoundException("User not found")
        );

        if (!courier.getRole().equals(Role.COURIER)){
            throw new DeliveryServiceExceptions.UserNotCourier("This user is not a courier");
        }

        if (!courier.getApprovalStatus().equals(ApprovalStatus.APPROVED)){
            throw new UserNotApprovedException("This courier is not approved");
        }

        ArrayList<OrderDto.OrderResponse> orders = new ArrayList<>();
        for (Order order : orderDAO.findOrdersHistoryByCourierId(courier.getId(), search, vendor, user, status)){
            orders.add(mapOrderToResponseDto(order));
        }

        return orders;
    }

    private OrderDto.OrderResponse mapOrderToResponseDto(Order order) {
        OrderDto.OrderResponse response = new OrderDto.OrderResponse();

        response.setId(order.getId());
        response.setDelivery_address(order.getDeliveryAddress());
        response.setCustomer_id(order.getCustomer().getId());
        response.setVendor_id(order.getRestaurant().getId());
        if (order.getCoupon() != null){
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

        if (order.getCourier() != null){
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
