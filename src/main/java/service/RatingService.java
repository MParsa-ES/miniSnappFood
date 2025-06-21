package service;

import dao.*;
import dto.MessageDto;
import dto.RatingDTO;
import entity.*;
import service.exception.OrderServiceExceptions;
import service.exception.UserNotFoundException;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class RatingService {
    private final UserDAO userDAO;
    private final RestaurantDAO restaurantDAO;
    private final FoodItemDAO foodItemDAO;
    private final OrderDAO orderDAO;
    private final RatingDAO ratingDAO;

    public RatingService(UserDAO userDAO, RestaurantDAO restaurantDAO, FoodItemDAO foodItemDAO, OrderDAO orderDAO, RatingDAO ratingDAO) {
        this.userDAO = userDAO;
        this.restaurantDAO = restaurantDAO;
        this.foodItemDAO = foodItemDAO;
        this.orderDAO = orderDAO;
        this.ratingDAO = ratingDAO;
    }


    public MessageDto SubmitRating (RatingDTO.Request requestDTO, String phone) throws OrderServiceExceptions, UserNotFoundException {

        User user = userDAO.findByPhone(phone)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        Order order = orderDAO.findOrderById(requestDTO.getOrder_id())
                .orElseThrow(() -> new OrderServiceExceptions.OrderNotFound("Order not found"));

        if (!order.getCustomer().equals(user)) {
            throw new OrderServiceExceptions.UserIsNotOwnerOfOrder("User is not owner of the order");
        }

//        if (!order.getStatus().toString().equals("COMPLETED")) {
//            throw new OrderServiceExceptions.OrderNotCompleted("Order is not completed");
//        }

        if(ratingDAO.doesRatingExist(user.getId(), order.getId())) {
            throw new OrderServiceExceptions.RatingAlreadyExists("Rating already exists");
        }

        int ratingPoint = requestDTO.getRating();

        Rating rating = new Rating(
                requestDTO.getRating(),
                requestDTO.getComment(),
                requestDTO.getImageBase64(),
                user,
                order
        );
        ratingDAO.save(rating);

        for(OrderItem orderItem: order.getItems()) {
            FoodItem foodItem = orderItem.getFoodItem();
            calculateAverageRating(foodItem, ratingPoint);
        }

        return new MessageDto("Rating submitted");

    }

    private void calculateAverageRating (FoodItem foodItem, int rating) {
        int num = foodItem.getNumberOfRatings();
        BigDecimal averageRating = foodItem.getAverageRating();
        BigDecimal totalRating = averageRating.multiply(new BigDecimal(num));
        totalRating = totalRating.add(BigDecimal.valueOf(rating));
        num++;
        averageRating = totalRating.divide(new BigDecimal(num), 2, RoundingMode.CEILING);
        foodItem.setAverageRating(averageRating);
        foodItem.setNumberOfRatings(num);
        foodItemDAO.update(foodItem);
    }

}
