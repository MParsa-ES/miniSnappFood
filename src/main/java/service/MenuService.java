package service;

import dao.FoodItemDAO;
import dao.MenuDAO;
import dao.RestaurantDAO;
import dao.UserDAO;
import dto.MenuDto;
import dto.MessageDto;
import entity.*;
import lombok.AllArgsConstructor;
import service.exception.MenuServiceExceptions;
import service.exception.RestaurantServiceExceptions;
import service.exception.UserNotFoundException;

@AllArgsConstructor
public class MenuService {

    private final UserDAO userDAO;
    private final MenuDAO menuDAO;
    private final RestaurantDAO restaurantDAO;
    private final FoodItemDAO foodItemDAO;


    public MenuDto.Response createMenu(MenuDto.Request request, String ownerUserPhone, Long restaurantId)
            throws MenuServiceExceptions.MenuIsDuplicateException, RestaurantServiceExceptions.UserNotOwner,
            RestaurantServiceExceptions.UserNotSeller, RestaurantServiceExceptions.RestaurantNotFound,
            UserNotFoundException {
        Restaurant restaurant = restaurantDAO.findRestaurantById(restaurantId).
                orElseThrow(() -> new RestaurantServiceExceptions.RestaurantNotFound("Restaurant not found"));
        User owner = userDAO.findByPhone(ownerUserPhone).orElseThrow(() -> new UserNotFoundException("User not found"));

        if (owner.getRole() != Role.SELLER) {
            throw new RestaurantServiceExceptions.UserNotSeller("Forbidden request");
        }

        if (!restaurant.getOwner().equals(owner)) {
            throw new RestaurantServiceExceptions.UserNotOwner("This user is not owner of this restaurant");
        }

        if (menuDAO.findMenuInRestaurant(request.getTitle(), restaurantId).isPresent()) {
            throw new MenuServiceExceptions.MenuIsDuplicateException("Menu already exists");
        }

        Menu menu = new Menu();
        menu.setRestaurant(restaurant);
        menu.setTitle(request.getTitle());
        menuDAO.save(menu);

        MenuDto.Response response = new MenuDto.Response();
        response.setTitle(request.getTitle());

        return response;
    }

    public MessageDto deleteMenu(String ownerPhoneNumber, Long restaurantId, String menuTitle) throws
            RestaurantServiceExceptions.RestaurantNotFound,
            UserNotFoundException, RestaurantServiceExceptions.UserNotSeller,
            RestaurantServiceExceptions.UserNotOwner, MenuServiceExceptions.MenuNotFoundException {

        Restaurant restaurant = restaurantDAO.findRestaurantById(restaurantId).orElseThrow(
                () -> new RestaurantServiceExceptions.RestaurantNotFound("Restaurant not found"));

        User owner = userDAO.findByPhone(ownerPhoneNumber).orElseThrow(
                () -> new UserNotFoundException("User not found"));

        if (owner.getRole() != Role.SELLER) {
            throw new RestaurantServiceExceptions.UserNotSeller("Forbidden request");
        }

        if (!restaurant.getOwner().equals(owner)) {
            throw new RestaurantServiceExceptions.UserNotOwner("This user is not owner of this restaurant");
        }


        Menu menu = menuDAO.findMenuInRestaurant(menuTitle, restaurantId).orElseThrow(
                () -> new MenuServiceExceptions.MenuNotFoundException("Menu with title " + menuTitle + " not found"));

        menuDAO.deleteById(menu.getId());

        return new MessageDto("Food menu removed from restaurant successfully");
    }

    public MessageDto addFoodToMenu(MenuDto.AddItemRequest requestDto,String ownerPhoneNumber, Long restaurantId, String menuTitle) throws
            RestaurantServiceExceptions.RestaurantNotFound,
            RestaurantServiceExceptions.UserNotSeller,
            RestaurantServiceExceptions.UserNotOwner, MenuServiceExceptions.MenuNotFoundException,
            UserNotFoundException, RestaurantServiceExceptions.ItemNotFound {

        Restaurant restaurant = restaurantDAO.findRestaurantById(restaurantId).orElseThrow(
                () -> new RestaurantServiceExceptions.RestaurantNotFound("Restaurant not found"));

        User owner = userDAO.findByPhone(ownerPhoneNumber).orElseThrow(
                () -> new UserNotFoundException("User not found"));

        if (owner.getRole() != Role.SELLER) {
            throw new RestaurantServiceExceptions.UserNotSeller("Forbidden request");
        }

        if (!restaurant.getOwner().equals(owner)) {
            throw new RestaurantServiceExceptions.UserNotOwner("This user is not owner of this restaurant");
        }

        Menu menu = menuDAO.findMenuInRestaurant(menuTitle, restaurantId).orElseThrow(
                () -> new MenuServiceExceptions.MenuNotFoundException("Menu with title " + menuTitle + " not found"));

        FoodItem foodItem = foodItemDAO.findFoodItemById(restaurantId, requestDto.getItem_id()).orElseThrow(
                () -> new RestaurantServiceExceptions.ItemNotFound("Item not Found"));

        if (menu.getFoodItems().add(foodItem)) {
            menuDAO.update(menu);
            return new MessageDto("Food item created and added to restaurant successfully");
        } else {
            return new MessageDto("Food item is already in the menu");
        }
    }
}
