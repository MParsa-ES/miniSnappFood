package dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CouponDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        private String coupon_code;
        private String type;
        private BigDecimal value;
        private BigDecimal min_price;
        private Integer user_count;
        private LocalDate start_date;
        private LocalDate end_date;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String coupon_code;
        private String type;
        private BigDecimal value;
        private BigDecimal min_price;
        private Integer user_count;
        private LocalDate start_date;
        private LocalDate end_date;
    }

}
