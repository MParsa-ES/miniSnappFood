package dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileDto {
    private String full_name;
    private String phone;
    private String email;
    private String role;
    private String address;
    private String profileImageBase64;
    private BankInfoDTO bank_info;

    @Getter
    @Setter
    public static class BankInfoDTO {
        private String bank_name;
        private String account_number;
    }

    public ProfileDto(String fullName, String phone, String email, String role, String address,
                      String profilePicture, String bankName, String accountNumber) {
        this.full_name = fullName;
        this.phone = phone;
        this.email = email;
        this.role = role;
        this.address = address;
        this.profileImageBase64 = profilePicture;
        this.bank_info = new BankInfoDTO();
        this.bank_info.bank_name = bankName;
        this.bank_info.account_number = accountNumber;
    }

}
