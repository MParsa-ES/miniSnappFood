package service.exception;

public class MenuServiceExceptions extends RuntimeException {
    private MenuServiceExceptions() {

    }

    public static class MenuIsDuplicateException extends RuntimeException {
        public MenuIsDuplicateException(String message) {
            super(message);
        }
    }

    public static class MenuNotFoundException extends RuntimeException {
        public MenuNotFoundException(String message) {
            super(message);
        }
    }

    public static class FoodNotInMenu extends RuntimeException {
        public FoodNotInMenu(String message) {
            super(message);
        }
    }
}
