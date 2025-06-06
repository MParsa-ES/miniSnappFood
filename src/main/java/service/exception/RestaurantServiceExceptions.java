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

    // Exception for when there is no owned restaurant by user
    public static class RestaurantNotFound extends RuntimeException {
        public RestaurantNotFound(String message) { super(message); }
    }

    // Exception for when a restaurant already exists
    public static class RestaurantAlreadyExists extends RuntimeException {
        public RestaurantAlreadyExists(String message) {
            super(message);
        }
    }

    public static class ItemNotFound extends RuntimeException {
        public ItemNotFound(String message) { super(message); }
    }

    public static class UserNotOwner extends RuntimeException {
        public UserNotOwner(String message) { super(message); }
    }
}

