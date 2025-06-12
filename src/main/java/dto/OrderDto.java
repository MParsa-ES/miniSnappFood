package dto;

import entity.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class OrderDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemRequest {
        private Long item_id;
        private int quantity;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        private String delivery_address;
        private Long vendor_id;
        private Long coupon_id;
        private ArrayList<OrderItemRequest> items;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderResponse {
        private Long id;
        private String delivery_address;
        private Long customer_id;
        private Long vendor_id;
        private Long coupon_id;
        private ArrayList<Long> item_ids;
        private BigDecimal raw_price;
        private BigDecimal tax_fee;
        private BigDecimal additional_fee;
        private BigDecimal courier_fee;
        private BigDecimal pay_price;
        private Long courier_id;
        private String status;
        private String created_at;
        private String updated_at;
    }
}
