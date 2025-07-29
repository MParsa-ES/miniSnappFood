package dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
public class RatingDTO {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        Long order_id;
        int rating;
        String comment;
        Set<String> imageBase64;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Update {
        int rating;
        String comment;
        Set<String> imageBase64;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ItemRatings {
        BigDecimal avg_rating;
        Set<Comments> comments;

        public static class Comments {
            Long id;
            Long item_id;
            int rating;
            String comment;
            Set<String> imageBase64;
            Long user_id;
            String username;
            String created_at;

            public Comments(Long id, Long item_id, int rating, String comment, Set<String> imageBase64, Long user_id, String username, String created_at) {
                this.id = id;
                this.item_id = item_id;
                this.rating = rating;
                this.comment = comment;
                this.imageBase64 = imageBase64;
                this.user_id = user_id;
                this.username = username;
                this.created_at = created_at;
            }

        }

        public ItemRatings(BigDecimal avg_rating, Set<Comments> comments) {
            this.avg_rating = avg_rating;
            this.comments = comments;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Rating {
        Long order_id;
        Set<Long> item_id;
        int rating;
        String comment;
        Set<String> imageBase64;
        Long user_id;
        String created_at;
    }

}
