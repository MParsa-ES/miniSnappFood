package service;

import dao.OrderDAO;
import dao.UserDAO;
import dto.OrderDto;
import entity.Order;
import entity.OrderItem;
import entity.Role;
import entity.User;
import service.exception.DeliveryServiceExceptions;
import service.exception.UserNotFoundException;

import java.util.ArrayList;

public class DeliveryService {

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

        ArrayList<OrderDto.OrderResponse> orders = new ArrayList<>();

        for (Order order : orderDAO.findOrdersAwaitingDelivery()){
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
