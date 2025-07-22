package service;

import dao.*;
import dto.*;
import entity.*;
import service.exception.*;

import java.time.LocalDate;
import java.util.*;

public class BuyerService {
    private final UserDAO userDAO;
    private final RestaurantDAO restaurantDAO;
    private final MenuDAO menuDAO;
    private final BuyerDAO buyerDAO;
    private final CouponDAO couponDAO;

    public BuyerService(UserDAO userDAO, RestaurantDAO restaurantDAO, MenuDAO menuDAO, BuyerDAO buyerDAO, CouponDAO couponDAO) {
        this.userDAO = userDAO;
        this.restaurantDAO = restaurantDAO;
        this.menuDAO = menuDAO;
        this.buyerDAO = buyerDAO;
        this.couponDAO = couponDAO;
    }

    public List<RestaurantDto.Response> getAllRestaurants() throws RuntimeException {

        List<Restaurant> restaurants = buyerDAO.getAllRestaurants();

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

    public List<RestaurantDto.Response> searchVendors(BuyerDto.ItemSearch requestDto) throws RuntimeException {

        List<Restaurant> restaurants = buyerDAO.searchVendorsByItemFilters(requestDto.getSearch(), requestDto.getMinPrice(), requestDto.getMaxPrice(), requestDto.getKeywords());

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

        for (Menu menu : restaurant.getMenus()) {
            List<FoodItemDto.Response> itemsDto = new ArrayList<>();
            menu_titles.add(menu.getTitle());

            for (FoodItem item : menu.getFoodItems()) {
                itemsDto.add(new FoodItemDto.Response(
                        item.getId(),
                        item.getName(),
                        item.getImageBase64(),
                        item.getDescription(),
                        restaurant.getId(),
                        item.getPrice(),
                        item.getSupply(),
                        item.getAverageRating(),
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
        for (FoodItem item : items) {
            responses.add(new FoodItemDto.Response(
                    item.getId(),
                    item.getName(),
                    item.getImageBase64(),
                    item.getDescription(),
                    item.getRestaurant().getId(),
                    item.getPrice(),
                    item.getSupply(),
                    item.getAverageRating(),
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
                foodItem.getAverageRating(),
                foodItem.getKeywords()
        );

    }

    public List<RestaurantDto.Response> getFavoriteRestaurants(String phone) throws RestaurantServiceExceptions {

        User user = buyerDAO.findUserWithFavoriteRestaurants(phone)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        List<RestaurantDto.Response> responses = new ArrayList<>();
        for (Restaurant restaurant : user.getFavoriteRestaurants()) {
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

    public MessageDto addFavoriteRestaurant(Long restaurantId, String phone) throws RestaurantServiceExceptions, MenuServiceExceptions {

        User user = buyerDAO.findUserWithFavoriteRestaurants(phone)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        Restaurant restaurant = restaurantDAO.findRestaurantById(restaurantId)
                .orElseThrow(() -> new RestaurantServiceExceptions.RestaurantNotFound("Restaurant not found"));

        if (user.getFavoriteRestaurants().contains(restaurant)) {
            throw new RestaurantServiceExceptions.RestaurantAlreadyFavorite("Restaurant already favorite");
        }

        user.addFavoriteRestaurant(restaurant);
        userDAO.update(user);
        return new MessageDto("Restaurant added to favourites successfully ");

    }

    public MessageDto removeFavoriteRestaurant(Long restaurantId, String phone) throws RestaurantServiceExceptions, MenuServiceExceptions {

        User user = buyerDAO.findUserWithFavoriteRestaurants(phone)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        Restaurant restaurant = restaurantDAO.findRestaurantById(restaurantId)
                .orElseThrow(() -> new RestaurantServiceExceptions.RestaurantNotFound("Restaurant not found"));

        if (!user.getFavoriteRestaurants().contains(restaurant)) {
            return new MessageDto("Restaurant is not favourite");
        }

        user.removeFavoriteRestaurant(restaurant);
        userDAO.update(user);
        return new MessageDto("Restaurant removed from favourites successfully ");

    }

    public List<MenuDto.Response> getRestaurantMenus(Long restaurantId) throws RestaurantServiceExceptions, MenuServiceExceptions {

        Restaurant restaurant = restaurantDAO.findRestaurantById(restaurantId)
                .orElseThrow(() -> new RestaurantServiceExceptions.RestaurantNotFound("Restaurant not found"));

        List<MenuDto.Response> responses = new ArrayList<>();

        for (Menu menu: restaurant.getMenus()) {
            responses.add(new MenuDto.Response(
                    menu.getId(),
                    menu.getTitle()
            ));
        }

        return responses;

    }

    public List<FoodItemDto.Response> getMenuItems(Long menuId) throws RestaurantServiceExceptions, MenuServiceExceptions {

        Menu menu = menuDAO.getMenuItems(menuId)
                .orElseThrow(() -> new MenuServiceExceptions.MenuNotFoundException("Menu not found"));

        Restaurant restaurant = menu.getRestaurant();
        List<FoodItemDto.Response> responses = new ArrayList<>();

        for (FoodItem foodItem: menu.getFoodItems()) {
            responses.add(new FoodItemDto.Response(
                    foodItem.getId(),
                    foodItem.getName(),
                    foodItem.getImageBase64(),
                    foodItem.getDescription(),
                    restaurant.getId(),
                    foodItem.getPrice(),
                    foodItem.getSupply(),
                    foodItem.getAverageRating(),
                    foodItem.getKeywords()
            ));
        }

        return responses;

    }

    public CouponDto.Response checkCoupon(String customerUserPhone, String couponCode) throws
            UserNotFoundException, CouponServiceExceptions.CouponNotFound,
            OrderServiceExceptions.UserNotBuyer, CouponServiceExceptions.InvalidCoupon {

        User customer = userDAO.findByPhone(customerUserPhone).orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!customer.getRole().equals(Role.BUYER)){
            throw new OrderServiceExceptions.UserNotBuyer("User not buyer");
        }

        Coupon coupon = couponDAO.findCouponByCouponCode(couponCode).orElseThrow(
                () -> new CouponServiceExceptions.CouponNotFound("Coupon with code " + couponCode + "not found")
        );

        if (coupon.getUserCount() <= 0) {
            throw new CouponServiceExceptions.InvalidCoupon("This coupon has no uses left");
        }

        if (LocalDate.now().isAfter(coupon.getEndDate())) {
            throw new CouponServiceExceptions.InvalidCoupon("This coupon has expired");
        }
        if (LocalDate.now().isBefore(coupon.getStartDate())) {
            throw new CouponServiceExceptions.InvalidCoupon("This coupon is not activated yet");
        }

        return mapCouponToResponseDto(coupon);

    }


    private CouponDto.Response mapCouponToResponseDto(Coupon coupon) {
        CouponDto.Response response = new CouponDto.Response();
        response.setId(coupon.getId());
        response.setCoupon_code(coupon.getCouponCode());
        response.setType(coupon.getCouponType().toString());
        response.setValue(coupon.getCouponValue());
        response.setMin_price(coupon.getMinPrice());
        response.setUser_count(coupon.getUserCount());
        response.setStart_date(coupon.getStartDate());
        response.setEnd_date(coupon.getEndDate());
        return response;
    }
}
