package HTTPhandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dao.*;
import dto.AdminDto;
import dto.CouponDto;
import dto.ErrorResponseDto;
import dto.TransactionDTO;
import entity.Order;
import jdk.jshell.execution.Util;
import service.AdminService;
import service.TransactionService;
import service.exception.AdminServiceExceptions;
import service.exception.CouponServiceExceptions;
import service.exception.UserNotFoundException;
import util.RateLimiter;
import util.Utils;
import util.LocalDateAdapter;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

public class AdminHTTPHandler implements HttpHandler {

    private final Gson gson = new GsonBuilder().
            registerTypeAdapter(LocalDate.class, new LocalDateAdapter()).serializeNulls().create();

    private final AdminService adminService;
    private final TransactionService transactionService;

    public AdminHTTPHandler() {
        this.adminService = new AdminService(new UserDAO(), new OrderDAO(), new CouponDAO());
        this.transactionService = new TransactionService(new UserDAO(), new RestaurantDAO(), new FoodItemDAO(), new OrderDAO(), new RatingDAO(), new TransactionDAO());
    }


    public void handle(HttpExchange exchange) throws IOException {

        String ip = exchange.getRemoteAddress().getAddress().getHostAddress();
        if (!RateLimiter.isAllowed(ip)) {
            Utils.sendResponse(exchange, 429, gson.toJson(new ErrorResponseDto("Too many requests")));
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();


        try {
            if (path.equals("/admin/users") && method.equals("GET")) {
                handleGetAllUsers(exchange);


            } else if (path.matches("^/admin/users/\\d+/status$") && method.equals("PATCH")) {
                Long userId = Long.parseLong(path.split("/")[3]);
                handleUpdateApprovalStatus(exchange, userId);


            } else if (path.matches("^/admin/users/\\d+/remove$")  && method.equals("DELETE")) {
                Long userId = Long.parseLong(path.split("/")[3]);
                handleDeleteUser(exchange, userId);


            } else if (path.equals("/admin/orders") && method.equals("GET")) {
                handleGetAllOrdersWithFilters(exchange);


            } else if (path.equals("/admin/coupons") && method.equals("POST")) {
                handleCreateCoupon(exchange);


            } else if (path.matches("^/admin/coupons/\\d+$") && method.equals("DELETE")) {
                Long couponId = Long.parseLong(path.split("/")[3]);
                handleDeleteCoupon(exchange, couponId);


            } else if (path.matches("^/admin/coupons/\\d+$") && method.equals("GET")) {
                Long couponId = Long.parseLong(path.split("/")[3]);
                handleGetCoupon(exchange, couponId);


            } else if (path.equals("/admin/coupons") && method.equals("GET")) {
                handleGetAllCoupons(exchange);


            } else if (path.matches("^/admin/coupons/\\d+$") && method.equals("PUT")) {
                Long couponId = Long.parseLong(path.split("/")[3]);
                handleUpdateCoupon(exchange, couponId);


            }  else if (path.equals("/admin/transactions") && "GET".equals(method)) {
                handleGetTransactions(exchange);


            } else {
                Utils.sendResponse(exchange, 404, gson.toJson(new ErrorResponseDto("Admin endpoint not found.")));


            }
        } catch (UserNotFoundException | CouponServiceExceptions.CouponNotFound e) {
            Utils.sendResponse(exchange, 404, gson.toJson(new ErrorResponseDto(e.getMessage())));
        } catch (AdminServiceExceptions.UserNotAdminException e) {
            Utils.sendResponse(exchange, 403, gson.toJson(new ErrorResponseDto(e.getMessage())));
        } catch (CouponServiceExceptions.DuplicateCouponCode e) {
            Utils.sendResponse(exchange, 409, gson.toJson(new ErrorResponseDto(e.getMessage())));
        } catch (IllegalArgumentException e) {
            Utils.sendResponse(exchange, 400, gson.toJson(new ErrorResponseDto(e.getMessage())));
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error in Admin HTTP Handler :" + e.getMessage());
            Utils.sendResponse(exchange, 500, gson.toJson(new ErrorResponseDto("Internal Server Error")));
        }

    }

    private void handleGetAllUsers(HttpExchange exchange) throws IOException {

        String AdminPhoneNumber = Utils.getAuthenticatedUserPhone(exchange);

        if (AdminPhoneNumber == null) {
            return;
        }

        Utils.sendResponse(exchange, 200, gson.toJson(adminService.getUsersList(AdminPhoneNumber)));
    }

    private void handleUpdateApprovalStatus(HttpExchange exchange, Long userId) throws IOException {

        if (Utils.checkUnathorizedMediaType(exchange)) {
            Utils.sendResponse(exchange, 415, gson.toJson(new ErrorResponseDto("Unauthorized Media Type")));
            return;
        }

        String adminUserName = Utils.getAuthenticatedUserPhone(exchange);

        if (adminUserName == null) {
            return;
        }

        AdminDto.UpdateUserApprovalDto requestDto;
        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            requestDto = gson.fromJson(reader, AdminDto.UpdateUserApprovalDto.class);

            if (requestDto == null || requestDto.getStatus() == null || requestDto.getStatus().isBlank()) {
                Utils.sendResponse(exchange, 400, gson.toJson(new ErrorResponseDto("Status field is missing")));
                return;
            }
        }

        Utils.sendResponse(exchange, 200, gson.toJson(adminService.updateUserApprovalStatus(adminUserName, requestDto, userId)));
    }

    private void handleDeleteUser(HttpExchange exchange, Long userId) throws IOException {
        String adminUserName = Utils.getAuthenticatedUserPhone(exchange);

        if (adminUserName == null) {
            return;
        }

        Utils.sendResponse(exchange, 200, gson.toJson(adminService.deleteUserFromSystem(adminUserName, userId)));
    }


    private void handleGetAllOrdersWithFilters(HttpExchange exchange) throws IOException {

        String adminUserName = Utils.getAuthenticatedUserPhone(exchange);
        if (adminUserName == null) {
            return;
        }

        String query = exchange.getRequestURI().getQuery();

        String search = null;
        String vendor = null;
        String courier = null;
        String customer = null;
        String status = null;


        if (query != null && !query.isEmpty()) {

            for (String filter : query.split("&")) {
                String[] keyValue = filter.split("=");
                if (keyValue.length == 2) {
                    switch (keyValue[0]) {
                        case "search":
                            search = java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                            break;
                        case "vendor":
                            vendor = java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                            break;
                        case "courier":
                            courier = java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                            break;
                        case "customer":
                            customer = java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                            break;
                        case "status":
                            status = java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                            break;
                    }
                }
            }
        }

        Utils.sendResponse(exchange, 200, gson.toJson(adminService.getOrdersList(adminUserName, search, vendor, courier, customer, status)));
    }

    private void handleCreateCoupon(HttpExchange exchange) throws IOException {

        if (Utils.checkUnathorizedMediaType(exchange)) {
            Utils.sendResponse(exchange, 415, gson.toJson(new ErrorResponseDto("Unauthorized Media Type")));
            return;
        }

        String adminUserName = Utils.getAuthenticatedUserPhone(exchange);
        if (adminUserName == null) {
            return;
        }

        CouponDto.Request requestDto;

        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            requestDto = gson.fromJson(reader, CouponDto.Request.class);

            if (requestDto == null) {
                throw new IllegalArgumentException("Invalid coupon request");
            }

            if (requestDto.getCoupon_code() == null || requestDto.getCoupon_code().isBlank()) {
                throw new IllegalArgumentException("Invalid coupon code");
            }
            if (requestDto.getValue() == null){
                throw new IllegalArgumentException("Invalid coupon value");
            }
            if (requestDto.getMin_price() == null) {
                throw new IllegalArgumentException("Invalid min price");
            }
            if (requestDto.getType() == null || requestDto.getType().isBlank()) {
                throw new IllegalArgumentException("Invalid coupon type");
            }
            if (requestDto.getUser_count() == null) {
                throw new IllegalArgumentException("Invalid coupon count");
            }
            if (requestDto.getStart_date() == null) {
                throw new IllegalArgumentException("Invalid coupon start date");
            }
            if (requestDto.getEnd_date() == null) {
                throw new IllegalArgumentException("Invalid coupon end date");
            }
        }

        Utils.sendResponse(exchange, 201, gson.toJson(adminService.createCoupon(adminUserName, requestDto)));


    }

    private void handleDeleteCoupon(HttpExchange exchange, Long couponId) throws IOException {

        String adminUserName = Utils.getAuthenticatedUserPhone(exchange);
        if (adminUserName == null) {
            return;
        }

        Utils.sendResponse(exchange, 200, gson.toJson(adminService.deleteCoupon(adminUserName, couponId)));
    }

    private void handleGetCoupon(HttpExchange exchange, Long couponId) throws IOException {

        String adminUserName = Utils.getAuthenticatedUserPhone(exchange);
        if (adminUserName == null) {
            return;
        }

        Utils.sendResponse(exchange, 200, gson.toJson(adminService.getCoupon(adminUserName, couponId)));
    }

    private void handleGetAllCoupons(HttpExchange exchange) throws IOException {
        String adminUserName = Utils.getAuthenticatedUserPhone(exchange);
        if (adminUserName == null) {
            return;
        }

        Utils.sendResponse(exchange, 200, gson.toJson(adminService.getCouponsList(adminUserName)));
    }

    private void handleUpdateCoupon(HttpExchange exchange, Long couponId) throws IOException {
        if (Utils.checkUnathorizedMediaType(exchange)) {
            Utils.sendResponse(exchange, 415, gson.toJson(new ErrorResponseDto("Unauthorized Media Type")));
            return;
        }

        String adminUserName = Utils.getAuthenticatedUserPhone(exchange);
        if (adminUserName == null) {
            return;
        }

        CouponDto.Request requestDto;
        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            requestDto = gson.fromJson(reader, CouponDto.Request.class);
            if (requestDto == null) {
                throw new IllegalArgumentException("Invalid coupon update request");
            }
        }

        Utils.sendResponse(exchange, 200, gson.toJson(adminService.updateCoupon(adminUserName, couponId, requestDto)));

    }

    private void handleGetTransactions(HttpExchange exchange) throws io.jsonwebtoken.io.IOException, java.io.IOException {
        String adminUserName = Utils.getAuthenticatedUserPhone(exchange);
        if (adminUserName == null) {
            return;
        }

        String query = exchange.getRequestURI().getQuery();

        String search = null;
        String user = null;
        String method = null;
        String status = null;


        if (query != null && !query.isEmpty()) {

            for (String filter : query.split("&")) {
                String[] keyValue = filter.split("=");
                if (keyValue.length == 2) {
                    switch (keyValue[0]) {
                        case "search":
                            search = java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                            break;
                        case "vendor":
                            user = java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                            break;
                        case "courier":
                            method = java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                            break;
                        case "status":
                            status = java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                            break;
                    }
                }
            }
        }

        Utils.sendResponse(exchange, 200, gson.toJson(transactionService.searchTransactions(adminUserName, search, user, method, status)));

    }

}
