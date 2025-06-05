package service.exception;

public class MenuServiceExceptions extends RuntimeException {
    private MenuServiceExceptions() {

    }

    public static class MenuIsDuplicateException extends RuntimeException {
        public MenuIsDuplicateException(String message) {
            super(message);
        }
    }
}
