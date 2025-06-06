package dto;


import entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RestaurantDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        private String name;
        private String address;
        private String phone;
        private String logoBase64;
        private int tax_fee;
        private int additional_fee;
        private User user;

    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String name;
        private String address;
        private String phone;
        private String logoBase64;
        private int tax_fee;
        private int additional_fee;

    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VendorRequest {
        private String search;
        private List<String> keywords;
    }

}
