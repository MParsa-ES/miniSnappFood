package service.exception;

public class OrderServiceExceptions extends RuntimeException {

    public static class UserNotBuyer extends RuntimeException {
       public UserNotBuyer(String message) {
         super(message);
       }
    }

    public static class ItemOutOfStock extends RuntimeException {
        public ItemOutOfStock(String message) {
            super(message);
        }
    }

    public static class OrderNotFound extends RuntimeException {
        public OrderNotFound(String message) {
            super(message);
        }
    }

    public static class NotOwnerOfOrder extends RuntimeException {
        public NotOwnerOfOrder(String message) {
            super(message);
        }
    }
}
