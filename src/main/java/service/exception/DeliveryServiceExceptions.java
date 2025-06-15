package service.exception;

public class DeliveryServiceExceptions extends RuntimeException {
    public static class UserNotCourier extends RuntimeException {
        public UserNotCourier(String message) {
            super(message);

        }
    }
}
