package service;

import dao.FoodItemDAO;
import dao.RestaurantDAO;
import dao.UserDAO;
import dto.FoodItemDto;
import dao.RestaurantDAO;
import entity.FoodItem;
import entity.Restaurant;
import entity.Role;
import entity.User;
import service.exception.RestaurantServiceExceptions;
import service.exception.UserNotFoundException;

public class FoodItemService {
    private final UserDAO userDAO;
    private final RestaurantDAO restaurantDAO;
    private final FoodItemDAO foodItemDAO;

    public FoodItemService(UserDAO userDAO, RestaurantDAO restaurantDAO, FoodItemDAO foodItemDAO) {
        this.userDAO = userDAO;
        this.restaurantDAO = restaurantDAO;
        this.foodItemDAO = foodItemDAO;
    }

    public FoodItemDto.Response createFoodItem(FoodItemDto.Request requestDto, String phone)
            throws UserNotFoundException,
            RestaurantServiceExceptions.UserNotSeller {

        User owner = userDAO.findByPhone(phone)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // if the user is not a seller
        if (owner.getRole() != Role.SELLER) {
            throw new RestaurantServiceExceptions.UserNotSeller("Forbidden request");
        }

        Restaurant restaurant = restaurantDAO.findRestaurant(owner)
                .orElseThrow(() -> new RestaurantServiceExceptions.RestaurantNotFound("Forbidden request"));

        FoodItem foodItem = new FoodItem();
        foodItem.setName(requestDto.getName());
        foodItem.setPrice(requestDto.getPrice());
        foodItem.setSupply(requestDto.getSupply());
        foodItem.setImageBase64(requestDto.getImageBase64());
        foodItem.setDescription(requestDto.getDescription());
        foodItem.setRestaurant(restaurant);
        foodItem.setKeywords(requestDto.getKeywords());

        FoodItem savedFood = foodItemDAO.save(foodItem);

        return new FoodItemDto.Response(
                savedFood.getId(),
                restaurant.getId(),
                savedFood.getName(),
                savedFood.getDescription(),
                savedFood.getPrice(),
                savedFood.getSupply(),
                savedFood.getImageBase64(),
                savedFood.getKeywords()
        );

    }




}
