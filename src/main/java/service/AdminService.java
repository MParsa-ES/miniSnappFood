package service;

import dao.UserDAO;
import dto.UserLoginDto;
import entity.Role;
import entity.User;
import entity.Profile;
import service.exception.AdminServiceExceptions;
import service.exception.UserNotFoundException;

import java.util.ArrayList;

public class AdminService {

    private final UserDAO userDAO;

    public AdminService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }


    public ArrayList<UserLoginDto.Response.UserData> getUsersList(String AdminPhoneNumber) throws
            UserNotFoundException, AdminServiceExceptions.UserNotAdminException {

        User admin = userDAO.findByPhone(AdminPhoneNumber).orElseThrow(
                () -> new UserNotFoundException("User not found")
        );

        if (!admin.getRole().equals(Role.ADMIN)) {
            throw new AdminServiceExceptions.UserNotAdminException("You are not admin");
        }

        ArrayList<UserLoginDto.Response.UserData> users = new ArrayList<>();

        for (User user : userDAO.getAllUsers()){
            users.add(mapToUserDataDto(user));
        }

        return users;

    }

    private UserLoginDto.Response.UserData mapToUserDataDto(User user){

        UserLoginDto.Response.UserData userData = new UserLoginDto.Response.UserData();
        userData.setId(user.getId().toString());
        userData.setFull_name(user.getFullName());
        userData.setPhone(user.getPhone());
        userData.setEmail(user.getEmail());
        userData.setAddress(user.getAddress());
        userData.setRole(user.getRole().toString());

        Profile profile = user.getProfile();

        if (profile != null) {
            userData.setProfileImageBase64(profile.getProfileImageBase64());


            if (profile.getBank_info() != null) {
                UserLoginDto.Response.UserData.BankInfoDto bankInfoDto = new UserLoginDto.Response.UserData.BankInfoDto();
                bankInfoDto.setBank_name(profile.getBank_info().getBankName());
                bankInfoDto.setAccount_number(profile.getBank_info().getAccountNumber());
                userData.setBank_info(bankInfoDto);
            }
        }

        return userData;
    }
}
