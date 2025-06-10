package dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.Set;

@Getter
@Setter
public class FoodItemDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        private String name;
        private String description;
        private int price;
        private int supply;
        private String imageBase64;
        private Set<String> keywords;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String name;
        private String imageBase64;
        private String description;
        private Long vendor_id;
        private int price;
        private int supply;
        private Set<String> keywords;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Update {
        private String name;
        private String imageBase64;
        private String description;
        private int price;
        private int supply;
        private Set<String> keywords;
    }

}