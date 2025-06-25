package service.exception;

public class AdminServiceExceptions extends RuntimeException {
    public static class UserNotAdminException extends RuntimeException {
        public UserNotAdminException(String message) {
            super(message);
        }
    }
}
