package service.exception;

public class RestaurantServiceExceptions extends RuntimeException {
    private RestaurantServiceExceptions() {

    }

    // Exception for when a user is not a seller (specific to the context of creating/managing a restaurant)
    public static class UserNotSeller extends RuntimeException {
        public UserNotSeller(String message) {
            super(message);
        }
    }

    // Exception for when a restaurant already exists
    public static class RestaurantAlreadyExists extends RuntimeException {
        public RestaurantAlreadyExists(String message) {
            super(message);
        }
    }

    // You could add other exceptions here in the future if they are
    // very specific to operations within RestaurantService.
}

