package dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRegisterRequestDto {
    private String full_name;
    private String phone;
    private String email;
    private String password;
    private String role;
    private String address;
    private String profileImageBase64;
    private BankInfoDto bank_info;


    @Getter
    @Setter
    public static class BankInfoDto {
        private String bank_name;
        private String account_number;
    }
}