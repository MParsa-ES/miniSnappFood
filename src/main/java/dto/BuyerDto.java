package dto;

import entity.Restaurant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
public class BuyerDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemList {

        private RestaurantDto.Response vendor;
        private List<String> menu_titles;
        private Map<String, List<FoodItemDto.Response>> menu_title;

    }

}
