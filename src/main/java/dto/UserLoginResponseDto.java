package dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserLoginResponseDto {
    private String message;
    private String token;
    private UserData user;

    @Getter
    @Setter
    public static class UserData {
        private String id;
        private String full_name;
        private String phone;
        private String email;
        private String role;
        private String address;
    }
}
