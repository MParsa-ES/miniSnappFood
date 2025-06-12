package service.exception;

public class OrderServiceExceptions extends RuntimeException {
    public static class UserNotBuyerException extends RuntimeException {
       public UserNotBuyerException(String message) {
         super(message);
       }
    }

    public static class ItemOutOfStockException extends RuntimeException {
        public ItemOutOfStockException(String message) {
            super(message);
        }
    }

    public static class OrderNotFoundException extends RuntimeException {
        public OrderNotFoundException(String message) {
            super(message);
        }
    }

    public static class NotOwnerOfOrderException extends RuntimeException {
        public NotOwnerOfOrderException(String message) {
            super(message);
        }
    }
}
