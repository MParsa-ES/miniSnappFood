package HTTPhandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dao.OrderDAO;
import dao.UserDAO;
import dto.AdminDto;
import dto.ErrorResponseDto;
import service.AdminService;
import service.exception.AdminServiceExceptions;
import service.exception.UserNotFoundException;
import util.RateLimiter;
import util.Utils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class AdminHTTPHandler implements HttpHandler {

    private final Gson gson = new GsonBuilder().serializeNulls().create();
    private final AdminService adminService;


    public AdminHTTPHandler() {
        this.adminService = new AdminService(new UserDAO(), new OrderDAO());

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


            } else if (path.equals("/admin/orders") && method.equals("GET")) {
                handleGetAllOrdersWithFilters(exchange);


            } else {
                Utils.sendResponse(exchange, 404, gson.toJson(new ErrorResponseDto("Admin endpoint not found.")));


            }
        } catch (UserNotFoundException e) {
            Utils.sendResponse(exchange, 404, gson.toJson(new ErrorResponseDto(e.getMessage())));
        } catch (AdminServiceExceptions.UserNotAdminException e) {
            Utils.sendResponse(exchange, 403, gson.toJson(new ErrorResponseDto(e.getMessage())));
        } catch (IllegalArgumentException e) {
            Utils.sendResponse(exchange, 400, gson.toJson(new ErrorResponseDto(e.getMessage())));
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error in Admin HTTP Handler :" + e.getMessage());
            Utils.sendResponse(exchange, 500, gson.toJson(new ErrorResponseDto("Internal Server Error")));
        }

    }

    private void handleGetAllUsers(HttpExchange exchange) throws IOException {

        String AdminPhoneNumebr = Utils.getAuthenticatedUserPhone(exchange);

        if (AdminPhoneNumebr == null) {
            return;
        }

        Utils.sendResponse(exchange, 200, gson.toJson(adminService.getUsersList(AdminPhoneNumebr)));
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


}
