package service;

import dao.BuyerDAO;
import dao.FoodItemDAO;
import dao.RestaurantDAO;
import dao.UserDAO;
import dto.RestaurantDto;
import entity.Restaurant;
import service.exception.RestaurantServiceExceptions;

import java.util.ArrayList;
import java.util.List;

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

}
