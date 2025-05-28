package service;

import dao.RestaurantDAO;
import dao.UserDAO;
import dto.RestaurantDto;
import entity.Restaurant;
import entity.User;
import entity.Role;
// Import the specific exceptions
import service.exception.UserNotFoundException;
import service.exception.RestaurantServiceExceptions; // Import the container

public class RestaurantService {
    private final UserDAO userDAO;
    private final RestaurantDAO restaurantDAO;

    public RestaurantService(UserDAO userDAO, RestaurantDAO restaurantDAO) {
        this.userDAO = userDAO;
        this.restaurantDAO = restaurantDAO;
    }

    public RestaurantDto.Response createRestaurant(RestaurantDto.Request requestDto, String ownerUserPhone)
            throws UserNotFoundException, // This is separate
            RestaurantServiceExceptions.UserNotSeller, // Inner class
            RestaurantServiceExceptions.RestaurantAlreadyExists { // Inner class

        User owner = userDAO.findByPhone(ownerUserPhone)
                .orElseThrow(() -> new UserNotFoundException("Owner user with phone " + ownerUserPhone + " not found."));

        if (owner.getRole() != Role.SELLER) {
            // Throw the nested exception
            throw new RestaurantServiceExceptions.UserNotSeller("User " + ownerUserPhone + " is not a seller and cannot create a restaurant.");
        }

        if (restaurantDAO.findByPhone(requestDto.getPhone()).isPresent()) {
            // Throw the nested exception
            throw new RestaurantServiceExceptions.RestaurantAlreadyExists("A restaurant with phone number " + requestDto.getPhone() + " already exists.");
        }

        Restaurant restaurant = new Restaurant();
        restaurant.setName(requestDto.getName());
        restaurant.setAddress(requestDto.getAddress());
        restaurant.setPhone(requestDto.getPhone());
        restaurant.setOwner(owner);

        restaurant.setLogo(requestDto.getLogoBase64()); // Can be null if DTO field is null
        restaurant.setTaxFee(requestDto.getTax_fee());
        restaurant.setAdditionalFee(requestDto.getAdditional_fee());

        Restaurant savedRestaurant = restaurantDAO.save(restaurant);

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
}