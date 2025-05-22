package dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileDto {
    private String fullName;
    private String phone;
    private String email;
    private String role;
    private String address;
    private String profilePicture;
    private BankInfoDTO bankInfo;

    @Getter
    @Setter
    public static class BankInfoDTO {
        private String bankName;
        private String accountNumber;
    }

    public ProfileDto(String fullName, String phone, String email, String role, String address,
                      String profilePicture, String bankName, String accountNumber) {
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.role = role;
        this.address = address;
        this.profilePicture = profilePicture;
        this.bankInfo = new BankInfoDTO();
        this.bankInfo.bankName = bankName;
        this.bankInfo.accountNumber = accountNumber;
    }

}
