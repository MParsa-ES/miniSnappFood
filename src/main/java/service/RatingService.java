package service;

import dao.*;
import dto.FoodItemDto;
import dto.MessageDto;
import dto.RatingDTO;
import entity.*;
import service.exception.OrderServiceExceptions;
import service.exception.RestaurantServiceExceptions;
import service.exception.UserNotFoundException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

        if (!order.getStatus().toString().equals("COMPLETED")) {
            throw new OrderServiceExceptions.OrderNotCompleted("Order is not completed");
        }

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
            calculateAverageRating(foodItem, ratingPoint, "plus");
        }

        return new MessageDto("Rating submitted");

    }


    public RatingDTO.ItemRatings getRatings (Long itemId, String phone) throws UserNotFoundException, RestaurantServiceExceptions {

        User user = userDAO.findByPhone(phone)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        FoodItem foodItem = foodItemDAO.findOnlyFoodItemById(itemId)
                .orElseThrow(() -> new RestaurantServiceExceptions.ItemNotFound("Item not found"));

        List<Rating> ratings = ratingDAO.getItemRatings(itemId);

        Set<RatingDTO.ItemRatings.Comments> commentsDTO = new HashSet<>();
        for(Rating rating: ratings) {
            commentsDTO.add(new RatingDTO.ItemRatings.Comments(
                rating.getId(),
                itemId,
                rating.getRating(),
                rating.getComment(),
                rating.getImages(),
                rating.getUser().getId(),
                rating.getUser().getFullName(),
                rating.getCreatedAt().toString()
            ));
        }

        return new RatingDTO.ItemRatings(foodItem.getAverageRating(), commentsDTO);

    }

    public boolean checkRating (Long orderId, String phone) throws UserNotFoundException, RestaurantServiceExceptions {

        User user = userDAO.findByPhone(phone)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        Order order = orderDAO.findOrderById(orderId)
                .orElseThrow(() -> new OrderServiceExceptions.OrderNotFound("Order not found"));

        return ratingDAO.doesRatingExist(user.getId(), order.getId());

    }

    public RatingDTO.Rating getRating (Long ratingId) throws RestaurantServiceExceptions {

        Rating rating = ratingDAO.getRatingById(ratingId)
                .orElseThrow(() -> new RestaurantServiceExceptions.RatingNotFound("Rating not found"));

        Set<Long> foodItemIds = new HashSet<>();
        for (OrderItem orderItem: rating.getOrder().getItems()) {
            foodItemIds.add(orderItem.getFoodItem().getId());
        }

        return new RatingDTO.Rating(
                ratingId,
                foodItemIds,
                rating.getRating(),
                rating.getComment(),
                rating.getImages(),
                rating.getUser().getId(),
                rating.getCreatedAt().toString()
        );

    }

    public MessageDto deleteRating (Long ratingId, String phone) throws UserNotFoundException, RestaurantServiceExceptions {

        User user = userDAO.findByPhone(phone)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        Rating rating = ratingDAO.getRatingById(ratingId)
                .orElseThrow(() -> new RestaurantServiceExceptions.RatingNotFound("Rating not found"));

        System.out.println(user.getId() + " " + rating.getId() + " " + rating.getUser().getId());

        if (!rating.getUser().getId().equals(user.getId())) {
            throw new RestaurantServiceExceptions.NotRatingOwner("Not owner of the rating");
        }

        Order order = rating.getOrder();
        int ratingPoint = rating.getRating();

        ratingDAO.delete(rating);

        for(OrderItem orderItem: order.getItems()) {
            FoodItem foodItem = orderItem.getFoodItem();
            calculateAverageRating(foodItem, ratingPoint, "minus");
        }

        return new MessageDto("Rating successfully deleted");

    }

    public MessageDto updateRating (RatingDTO.Update requestDTO, Long ratingId, String phone) throws UserNotFoundException, RestaurantServiceExceptions {

        Rating rating = ratingDAO.getRatingById(ratingId)
                .orElseThrow(() -> new RestaurantServiceExceptions.RatingNotFound("Rating not found"));

        User user = userDAO.findByPhone(phone)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if(!rating.getUser().getId().equals(user.getId())) {
            throw new RestaurantServiceExceptions.NotRatingOwner("Not owner of the rating");
        }

        Order order = rating.getOrder();
        int oldRatingPoint = rating.getRating();
        int newRatingPoint = requestDTO.getRating();


        if (requestDTO.getComment() != null) {
            rating.setComment(requestDTO.getComment());
        }
        if (requestDTO.getImageBase64() != null) {
            rating.setImages(requestDTO.getImageBase64());
        }
        if (newRatingPoint != 0) {
            rating.setRating(newRatingPoint);
            int ratingPoint = (newRatingPoint - oldRatingPoint);
            for (OrderItem orderItem: order.getItems()) {
                FoodItem foodItem = orderItem.getFoodItem();
                calculateAverageRating(foodItem, ratingPoint, "update");
            }
        }

        ratingDAO.update(rating);
        return new MessageDto("Rating successfully updated");

    }



    private void calculateAverageRating (FoodItem foodItem, int rating, String operation) {
        int num = foodItem.getNumberOfRatings();
        BigDecimal averageRating = foodItem.getAverageRating();
        BigDecimal totalRating = averageRating.multiply(new BigDecimal(num));
        if (operation.equals("plus")) {
            totalRating = totalRating.add(BigDecimal.valueOf(rating));
            num++;
            averageRating = totalRating.divide(new BigDecimal(num), 2, RoundingMode.CEILING);
        } else if (operation.equals("minus")) {
            totalRating = totalRating.subtract(BigDecimal.valueOf(rating));
            num--;
            averageRating = totalRating.divide(new BigDecimal(num), 2, RoundingMode.CEILING);
        } else if (operation.equals("update")) {
            totalRating = totalRating.add(BigDecimal.valueOf(rating));
            averageRating = totalRating.divide(new BigDecimal(num), 2, RoundingMode.CEILING);
        }
        foodItem.setAverageRating(averageRating);
        foodItem.setNumberOfRatings(num);
        foodItemDAO.update(foodItem);
    }

}
