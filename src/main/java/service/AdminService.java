package service;

import dao.OrderDAO;
import dao.UserDAO;
import dto.AdminDto;
import dto.MessageDto;
import dto.OrderDto;
import dto.UserLoginDto;
import entity.ApprovalStatus;
import entity.Role;
import entity.User;
import entity.Profile;
import service.exception.AdminServiceExceptions;
import service.exception.UserNotFoundException;

import java.util.ArrayList;

public class AdminService {

    private final UserDAO userDAO;
    private final OrderDAO orderDAO;

    public AdminService(UserDAO userDAO, OrderDAO orderDAO) {
        this.userDAO = userDAO;
        this.orderDAO = orderDAO;
    }


    public ArrayList<UserLoginDto.Response.UserData> getUsersList(String adminUserName) throws
            UserNotFoundException, AdminServiceExceptions.UserNotAdminException {

        User admin = userDAO.findByPhone(adminUserName).orElseThrow(
                () -> new UserNotFoundException("User not found")
        );

        if (!admin.getRole().equals(Role.ADMIN)) {
            throw new AdminServiceExceptions.UserNotAdminException("You are not admin");
        }

        ArrayList<UserLoginDto.Response.UserData> users = new ArrayList<>();

        for (User user : userDAO.getAllUsers()) {
            users.add(mapToUserDataDto(user));
        }

        return users;

    }

    public MessageDto updateUserApprovalStatus(String adminUserName, AdminDto.UpdateUserApprovalDto requestDto, Long userId) throws
            UserNotFoundException, AdminServiceExceptions.UserNotAdminException, IllegalArgumentException {

        User admin = userDAO.findByPhone(adminUserName).orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!admin.getRole().equals(Role.ADMIN)) {
            throw new AdminServiceExceptions.UserNotAdminException("You are not admin");
        }

        User user = userDAO.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found"));

        if (admin.getId().equals(user.getId())) {
            throw new IllegalArgumentException("Admins cannot change their own approval status");
        }

        ApprovalStatus newStatus;

        try {
            newStatus = ApprovalStatus.valueOf(requestDto.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid approval status");
        }

        user.setApprovalStatus(newStatus);
        userDAO.update(user);
        return new MessageDto("Status updated");

    }

    public ArrayList<OrderDto.OrderResponse> getOrdersList(String adminUserName){

        User admin = userDAO.findByPhone(adminUserName).orElseThrow(
                () -> new UserNotFoundException("User not found")
        );

        ArrayList<OrderDto.OrderResponse> orders = new ArrayList<>();


    }

    private UserLoginDto.Response.UserData mapToUserDataDto(User user) {

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
