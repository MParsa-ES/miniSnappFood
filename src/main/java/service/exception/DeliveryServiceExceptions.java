package service.exception;

public class DeliveryServiceExceptions extends RuntimeException {
    public static class UserNotCourier extends RuntimeException {
        public UserNotCourier(String message) {
            super(message);

        }
    }

    public static class OrderAlreadyAssignedToCourier extends RuntimeException {
        public OrderAlreadyAssignedToCourier(String message) {
            super(message);
        }
    }

    public static class OrderNotReadyForDelivery extends RuntimeException {
        public OrderNotReadyForDelivery(String message) {
            super(message);
        }
    }

    public static class CourierIsBusy extends RuntimeException {
        public CourierIsBusy(String message) {
            super(message);
        }
    }

    public static class CourierNotApproved extends RuntimeException {
        public CourierNotApproved(String message) {
            super(message);
        }
    }
}
