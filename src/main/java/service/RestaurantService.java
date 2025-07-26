package service;

import dao.RestaurantDAO;
import dao.UserDAO;
import dto.RestaurantDto;
import entity.ApprovalStatus;
import entity.Restaurant;
import entity.User;
import entity.Role;
import service.exception.UserNotApprovedException;
import service.exception.UserNotFoundException;
import service.exception.RestaurantServiceExceptions;

import java.util.ArrayList;
import java.util.NoSuchElementException;

public class RestaurantService {
    private final UserDAO userDAO;
    private final RestaurantDAO restaurantDAO;

    public RestaurantService(UserDAO userDAO, RestaurantDAO restaurantDAO) {
        this.userDAO = userDAO;
        this.restaurantDAO = restaurantDAO;
    }

    public RestaurantDto.Response createRestaurant(RestaurantDto.Request requestDto, String ownerUserPhone)
            throws UserNotFoundException,
            RestaurantServiceExceptions.UserNotSeller,
            RestaurantServiceExceptions.RestaurantAlreadyExists {

        User owner = userDAO.findByPhone(ownerUserPhone)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // if the user is not a seller
        if (owner.getRole() != Role.SELLER) {
            throw new RestaurantServiceExceptions.UserNotSeller("Forbidden request");
        }

        if (!owner.getApprovalStatus().equals(ApprovalStatus.APPROVED)) {
            throw new UserNotApprovedException("This seller is not approved");
        }

        // a restaurant with this number already exists
        if (restaurantDAO.findByPhone(requestDto.getPhone()).isPresent()) {
            throw new RestaurantServiceExceptions.RestaurantAlreadyExists("Conflict occurred");
        }

        Restaurant restaurant = new Restaurant();
        restaurant.setName(requestDto.getName());
        restaurant.setAddress(requestDto.getAddress());
        restaurant.setPhone(requestDto.getPhone());
        restaurant.setOwner(owner);
        restaurant.setLogo(requestDto.getLogoBase64());
        restaurant.setTaxFee(requestDto.getTax_fee());
        restaurant.setAdditionalFee(requestDto.getAdditional_fee());

        Restaurant savedRestaurant = restaurantDAO.save(restaurant);

        // return the response dto for restaurant
        return new RestaurantDto.Response(
                savedRestaurant.getId(),
                savedRestaurant.getName(),
                savedRestaurant.getAddress(),
                savedRestaurant.getPhone(),
                savedRestaurant.getLogo(),
                savedRestaurant.getTaxFee(),
                savedRestaurant.getAdditionalFee()
        );
    }

    public ArrayList<RestaurantDto.Response> listRestaurants(String ownerUserPhone)
            throws RestaurantServiceExceptions.UserNotSeller {

        User owner = userDAO.findByPhone(ownerUserPhone)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (owner.getRole() != Role.SELLER) {
            throw new RestaurantServiceExceptions.UserNotSeller("Forbidden request");
        }

        if (!owner.getApprovalStatus().equals(ApprovalStatus.APPROVED)) {
            throw new UserNotApprovedException("You are not approved yet");
        }

        Restaurant ownerRestaurant = null;
        ArrayList<RestaurantDto.Response> restaurants = new ArrayList<>();


        try {
            ownerRestaurant = restaurantDAO.findRestaurantByOwner(owner).get();
        } catch (NoSuchElementException e) {
            return restaurants;
        }

        restaurants.add(new RestaurantDto.Response(
                ownerRestaurant.getId(),
                ownerRestaurant.getName(),
                ownerRestaurant.getAddress(),
                ownerRestaurant.getPhone(),
                ownerRestaurant.getLogo(),
                ownerRestaurant.getTaxFee(),
                ownerRestaurant.getAdditionalFee()
        ));

        return restaurants;

    }

    public RestaurantDto.Response updateRestaurant(RestaurantDto.Request requestDto, String ownerUserPhone, Long restaurantId)
            throws UserNotFoundException, RestaurantServiceExceptions.UserNotSeller,
            RestaurantServiceExceptions.RestaurantNotFound,
            RestaurantServiceExceptions.RestaurantAlreadyExists,
            RestaurantServiceExceptions.UserNotOwner {

        Restaurant currentRestaurant;
        try {
            currentRestaurant = restaurantDAO.findRestaurantById(restaurantId).get();
        } catch (NoSuchElementException e) {
            throw new RestaurantServiceExceptions.RestaurantNotFound("Restaurant not found");
        }

        User owner = userDAO.findByPhone(ownerUserPhone)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (owner.getRole() != Role.SELLER) {
            throw new RestaurantServiceExceptions.UserNotSeller("User is not seller");
        }

        if (!currentRestaurant.getOwner().equals(owner)) {
            throw new RestaurantServiceExceptions.UserNotOwner("Seller is not the owner of this restaurant");
        }

        if (!owner.getApprovalStatus().equals(ApprovalStatus.APPROVED)) {
            throw new UserNotApprovedException("This seller is not approved");
        }

        if (requestDto.getPhone() != null && !requestDto.getPhone().equals(currentRestaurant.getPhone()) && restaurantDAO.findByPhone(requestDto.getPhone()).isPresent()) {
            throw new RestaurantServiceExceptions.RestaurantAlreadyExists("Conflict occurred");
        }

        if (requestDto.getName() != null) currentRestaurant.setName(requestDto.getName());
        if (requestDto.getPhone() != null) currentRestaurant.setPhone(requestDto.getPhone());
        if (requestDto.getAddress() != null) currentRestaurant.setAddress(requestDto.getAddress());
        if (requestDto.getLogoBase64() != null) currentRestaurant.setLogo(requestDto.getLogoBase64());
        if (requestDto.getTax_fee() != 0) currentRestaurant.setTaxFee(requestDto.getTax_fee());
        if (requestDto.getAdditional_fee() != 0) currentRestaurant.setAdditionalFee(requestDto.getAdditional_fee());

        Restaurant updatedRestaurant = restaurantDAO.update(currentRestaurant);

        return new RestaurantDto.Response(
                updatedRestaurant.getId(),
                updatedRestaurant.getName(),
                updatedRestaurant.getAddress(),
                updatedRestaurant.getPhone(),
                updatedRestaurant.getLogo(),
                updatedRestaurant.getTaxFee(),
                updatedRestaurant.getAdditionalFee()
        );

    }

    public RestaurantDto.Response getRestaurantBytId(String courierUserPhone, Long restaurantId) {
        User courier = userDAO.findByPhone(courierUserPhone).orElseThrow(
                () -> new UserNotFoundException("User not found")
        );

        if (!courier.getRole().equals(Role.COURIER) && !courier.getRole().equals(Role.SELLER)) {
            throw new RestaurantServiceExceptions.UserNotSeller("User is not seller or courier");
        }
        Restaurant restaurant = restaurantDAO.findRestaurantById(restaurantId).orElseThrow(
                () -> new RestaurantServiceExceptions.RestaurantNotFound("Restaurant not found")
        );

        RestaurantDto.Response restaurantDto = new RestaurantDto.Response(
                restaurant.getId(),restaurant.getName(), restaurant.getAddress(), restaurant.getPhone(), restaurant.getLogo(), restaurant.getTaxFee(), restaurant.getAdditionalFee());

        return restaurantDto;
    }
}