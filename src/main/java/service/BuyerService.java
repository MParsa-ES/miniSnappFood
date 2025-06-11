package service;

import dao.BuyerDAO;
import dao.FoodItemDAO;
import dao.RestaurantDAO;
import dao.UserDAO;
import dto.BuyerDto;
import dto.FoodItemDto;
import dto.RestaurantDto;
import entity.FoodItem;
import entity.Menu;
import entity.Restaurant;
import entity.User;
import service.exception.MenuServiceExceptions;
import service.exception.RestaurantServiceExceptions;
import service.exception.UserNotFoundException;

import java.util.*;

public class BuyerService {
    private final UserDAO userDAO;
    private final RestaurantDAO restaurantDAO;
    private final FoodItemDAO foodItemDAO;
    private final BuyerDAO buyerDAO;

    public BuyerService(UserDAO userDAO, RestaurantDAO restaurantDAO, FoodItemDAO foodItemDAO, BuyerDAO buyerDAO) {
        this.userDAO = userDAO;
        this.restaurantDAO = restaurantDAO;
        this.foodItemDAO = foodItemDAO;
        this.buyerDAO = buyerDAO;
    }

    public List<RestaurantDto.Response> GetVendorsList(String search, List<String> keywords) throws RestaurantServiceExceptions {

        List<Restaurant> restaurants = buyerDAO.SearchVendors(search, keywords);
        List<RestaurantDto.Response> responses = new ArrayList<>();
        for (Restaurant restaurant : restaurants) {
            responses.add(new RestaurantDto.Response(
                    restaurant.getId(),
                    restaurant.getName(),
                    restaurant.getAddress(),
                    restaurant.getPhone(),
                    restaurant.getLogo(),
                    restaurant.getTaxFee(),
                    restaurant.getAdditionalFee()
            ));
        }
        return responses;
    }

    public BuyerDto.ItemList GetItemList(Long restaurantId) throws RestaurantServiceExceptions,
            MenuServiceExceptions {

        Restaurant restaurant = buyerDAO.findVendorWithMenuAndItems(restaurantId)
                .orElseThrow(() -> new RestaurantServiceExceptions.RestaurantNotFound("Restaurant not found"));


        RestaurantDto.Response vendorDto = new RestaurantDto.Response(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getAddress(),
                restaurant.getPhone(),
                restaurant.getLogo(),
                restaurant.getTaxFee(),
                restaurant.getAdditionalFee()
        );

        List<String> menu_titles = new ArrayList<>();
        Map<String, List<FoodItemDto.Response>> menusMap = new HashMap<>();

        for (Menu menu: restaurant.getMenus()){
            List<FoodItemDto.Response> itemsDto = new ArrayList<>();
            menu_titles.add(menu.getTitle());

            for (FoodItem item: menu.getFoodItems()){
                itemsDto.add(new FoodItemDto.Response(
                        item.getId(),
                        item.getName(),
                        item.getImageBase64(),
                        item.getDescription(),
                        restaurant.getId(),
                        item.getPrice(),
                        item.getSupply(),
                        item.getKeywords()
                ));
            }
            menusMap.put(menu.getTitle(), itemsDto);
        }

        return new BuyerDto.ItemList(vendorDto, menu_titles, menusMap);

    }

    public List<FoodItemDto.Response> GetItemsList(String search, int price, List<String> keywords) throws RestaurantServiceExceptions {

        List<FoodItem> items = buyerDAO.getItemList(search, price, keywords);
        List<FoodItemDto.Response> responses = new ArrayList<>();
        for (FoodItem item: items) {
            responses.add(new FoodItemDto.Response(
                    item.getId(),
                    item.getName(),
                    item.getImageBase64(),
                    item.getDescription(),
                    item.getRestaurant().getId(),
                    item.getPrice(),
                    item.getSupply(),
                    item.getKeywords()
            ));
        }
        return responses;
    }

    public FoodItemDto.Response GetItem(Long id) throws RestaurantServiceExceptions, MenuServiceExceptions {

        FoodItem foodItem = buyerDAO.findItem(id)
                .orElseThrow(() -> new RestaurantServiceExceptions.ItemNotFound("Item not Found"));

        return new FoodItemDto.Response(
                foodItem.getId(),
                foodItem.getName(),
                foodItem.getImageBase64(),
                foodItem.getDescription(),
                foodItem.getRestaurant().getId(),
                foodItem.getPrice(),
                foodItem.getSupply(),
                foodItem.getKeywords()
        );

    }

    public List<RestaurantDto.Response> getFavouriteRestaurants(String phone) throws RestaurantServiceExceptions {

        User user = buyerDAO.findUSerWithFavouriteRestaurants(phone)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        List<RestaurantDto.Response> responses = new ArrayList<>();
        for(Restaurant restaurant: user.getFavoriteRestaurants()){
            responses.add(new RestaurantDto.Response(
                    restaurant.getId(),
                    restaurant.getName(),
                    restaurant.getAddress(),
                    restaurant.getPhone(),
                    restaurant.getLogo(),
                    restaurant.getTaxFee(),
                    restaurant.getAdditionalFee()
            ));
        }

        return responses;

    }

}
