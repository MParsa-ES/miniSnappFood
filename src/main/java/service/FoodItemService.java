package service;

import dao.FoodItemDAO;
import dao.RestaurantDAO;
import dao.UserDAO;
import dto.FoodItemDto;
import dao.RestaurantDAO;
import dto.RestaurantDto;
import entity.FoodItem;
import entity.Restaurant;
import entity.Role;
import entity.User;
import service.exception.RestaurantServiceExceptions;
import service.exception.UserNotFoundException;

import java.util.NoSuchElementException;

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

    public FoodItemDto.Update UpdateItem(FoodItemDto.Request requestDto, String ownerUserPhone, Long restaurantId, Long itemId)
            throws UserNotFoundException, RestaurantServiceExceptions.UserNotSeller,
            RestaurantServiceExceptions.RestaurantNotFound,
            RestaurantServiceExceptions.RestaurantAlreadyExists,
            RestaurantServiceExceptions.UserNotOwner {


        User owner = userDAO.findByPhone(ownerUserPhone)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (owner.getRole() != Role.SELLER) {
            throw new RestaurantServiceExceptions.UserNotSeller("Forbidden request");
        }

        Restaurant currentRestaurant = restaurantDAO.findRestaurantById(restaurantId)
                .orElseThrow(() -> new RestaurantServiceExceptions.RestaurantNotFound("Restaurant not found"));

        FoodItem currentFoodItem = foodItemDAO.findFoodItemById(itemId, restaurantId)
                .orElseThrow(() -> new RestaurantServiceExceptions.RestaurantNotFound("Item not Found"));

        if (!currentRestaurant.getOwner().equals(owner)) {
            throw new RestaurantServiceExceptions.UserNotOwner("Forbidden request");
        }


        if (requestDto.getName() != null) currentFoodItem.setName(requestDto.getName());
        if (requestDto.getImageBase64() != null) currentFoodItem.setImageBase64(requestDto.getImageBase64());
        if (requestDto.getDescription() != null) currentFoodItem.setDescription(requestDto.getDescription());
        if (requestDto.getPrice() != 0) currentFoodItem.setPrice(requestDto.getPrice());
        if (requestDto.getSupply() != 0) currentFoodItem.setSupply(requestDto.getSupply());
        if (requestDto.getKeywords() != null) currentFoodItem.setKeywords(requestDto.getKeywords());

        FoodItem updatedFoodItem = foodItemDAO.update(currentFoodItem);

        return new FoodItemDto.Update(
                updatedFoodItem.getName(),
                updatedFoodItem.getImageBase64(),
                updatedFoodItem.getDescription(),
                updatedFoodItem.getPrice(),
                updatedFoodItem.getSupply(),
                updatedFoodItem.getKeywords()
        );

    }


}
