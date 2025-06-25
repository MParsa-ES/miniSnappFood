package service;

import dao.CouponDAO;
import dao.OrderDAO;
import dao.UserDAO;
import dto.*;
import entity.*;
import service.exception.AdminServiceExceptions;
import service.exception.CouponServiceExceptions;
import service.exception.UserNotFoundException;

import java.util.ArrayList;

public class AdminService {

    private final UserDAO userDAO;
    private final OrderDAO orderDAO;
    private final CouponDAO couponDAO;

    public AdminService(UserDAO userDAO, OrderDAO orderDAO, CouponDAO couponDAO) {
        this.userDAO = userDAO;
        this.orderDAO = orderDAO;
        this.couponDAO = couponDAO;
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

    public ArrayList<OrderDto.OrderResponse> getOrdersList(String adminUserName, String search, String vendor, String courier, String customer, String status){

        User admin = userDAO.findByPhone(adminUserName).orElseThrow(
                () -> new UserNotFoundException("User not found")
        );

        if (!admin.getRole().equals(Role.ADMIN)) {
            throw new AdminServiceExceptions.UserNotAdminException("You are not admin");
        }

        ArrayList<OrderDto.OrderResponse> orderResponses = new ArrayList<>();

        for (Order order : orderDAO.getAllOrdersWithFilters(search, vendor, courier, customer, status)) {
            orderResponses.add(mapOrderToResponseDto(order));
        }

        return orderResponses;

    }

    public CouponDto.Response createCoupon(String adminUserName, CouponDto.CreateRequest requestDto) throws
            IllegalArgumentException, UserNotFoundException, AdminServiceExceptions.UserNotAdminException,
            CouponServiceExceptions.DuplicateCouponCode {

        User admin = userDAO.findByPhone(adminUserName).orElseThrow(
                () -> new UserNotFoundException("User not found")
        );

        if (!admin.getRole().equals(Role.ADMIN)) {
            throw new AdminServiceExceptions.UserNotAdminException("You are not admin");
        }

        if (couponDAO.findCouponByCode(requestDto.getCoupon_code()).isPresent()){
            throw new CouponServiceExceptions.DuplicateCouponCode("Coupon code already exists");
        }

        Coupon coupon = new Coupon();

        coupon.setCouponCode(requestDto.getCoupon_code());
        try {
            coupon.setCouponType(CouponType.valueOf(requestDto.getType().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid coupon type");
        }
        coupon.setCouponValue(requestDto.getValue());
        coupon.setMinPrice(requestDto.getMin_price());
        coupon.setUserCount(requestDto.getUser_count());
        coupon.setStartDate(requestDto.getStart_date());
        coupon.setEndDate(requestDto.getEnd_date());


        if (coupon.getStartDate().isAfter(coupon.getEndDate())) {
            throw new IllegalArgumentException("Coupon start date is after end date");
        }
        if (coupon.getStartDate().isEqual(coupon.getEndDate())) {
            throw new IllegalArgumentException("Coupon start date is equal with end date");
        }
        if (coupon.getCouponType().equals(CouponType.PERCENT) && coupon.getCouponValue().intValue() > 100){
            throw new IllegalArgumentException("Coupon value is greater than 100%");
        }
        if (coupon.getUserCount() <= 0){
            throw new IllegalArgumentException("Coupon user count must be greater than 0");
        }

        couponDAO.save(coupon);

        return mapCouponToResponseDto(coupon);

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

    private OrderDto.OrderResponse mapOrderToResponseDto(Order order) {
        OrderDto.OrderResponse response = new OrderDto.OrderResponse();

        response.setId(order.getId());
        response.setDelivery_address(order.getDeliveryAddress());
        response.setCustomer_id(order.getCustomer().getId());
        response.setVendor_id(order.getRestaurant().getId());
        // TODO: set the coupon id
        response.setRaw_price(order.getRawPrice());
        response.setTax_fee(order.getTaxFee());
        response.setAdditional_fee(order.getAdditionalFee());
        response.setCourier_fee(order.getCourierFee());
        response.setPay_price(order.getTotalPrice());
        response.setStatus(order.getStatus().name());
        response.setCreated_at(order.getCreatedAt().toString());
        response.setUpdated_at(order.getUpdatedAt().toString());

        if (order.getCourier() != null){
            response.setCourier_id(order.getCourier().getId());
        }

        ArrayList<Long> itemIds = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            itemIds.add(item.getFoodItem().getId());
        }
        response.setItem_ids(itemIds);

        return response;
    }

    private CouponDto.Response mapCouponToResponseDto(Coupon coupon) {
        CouponDto.Response response = new CouponDto.Response();
        response.setId(coupon.getId());
        response.setCoupon_code(coupon.getCouponCode());
        response.setType(coupon.getCouponType().toString());
        response.setValue(coupon.getCouponValue());
        response.setMin_price(coupon.getMinPrice());
        response.setUser_count(coupon.getUserCount());
        response.setStart_date(coupon.getStartDate());
        response.setEnd_date(coupon.getEndDate());
        return response;
    }
}
