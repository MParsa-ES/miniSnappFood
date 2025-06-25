package service.exception;

public class CouponServiceExceptions extends RuntimeException {

    public static class DuplicateCouponCode extends RuntimeException {
      public DuplicateCouponCode(String message) {
        super(message);
      }
    }

}
