package HTTPhandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dao.UserDAO;
import dto.ErrorResponseDto;
import service.AdminService;
import service.exception.AdminServiceExceptions;
import service.exception.UserNotFoundException;
import util.RateLimiter;
import util.Utils;

import java.io.IOException;

public class AdminHTTPHandler implements HttpHandler {

    private final Gson gson = new GsonBuilder().serializeNulls().create();
    private final AdminService adminService;


    public AdminHTTPHandler() {
        this.adminService = new AdminService(new UserDAO());

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


            } else {
                Utils.sendResponse(exchange, 404, gson.toJson(new ErrorResponseDto("Admin endpoint not found.")));


            }
        } catch (UserNotFoundException e) {
            Utils.sendResponse(exchange, 404, gson.toJson(new ErrorResponseDto(e.getMessage())));
        } catch (AdminServiceExceptions.UserNotAdminException e){
            Utils.sendResponse(exchange, 403, gson.toJson(new ErrorResponseDto(e.getMessage())));
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


}
