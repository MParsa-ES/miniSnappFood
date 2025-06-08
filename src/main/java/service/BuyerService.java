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
import service.exception.MenuServiceExceptions;
import service.exception.RestaurantServiceExceptions;

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

}
