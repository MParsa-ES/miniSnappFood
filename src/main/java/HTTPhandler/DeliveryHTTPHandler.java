package HTTPhandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dao.OrderDAO;
import dao.UserDAO;
import dto.ErrorResponseDto;
import service.DeliveryService;
import service.exception.DeliveryServiceExceptions;
import service.exception.UserNotFoundException;
import util.RateLimiter;
import util.Utils;

import java.io.IOException;

public class DeliveryHTTPHandler implements HttpHandler {

    private final Gson gson = new GsonBuilder().serializeNulls().create();
    private final DeliveryService deliveryService;


    public DeliveryHTTPHandler() {
        this.deliveryService = new DeliveryService(
                new UserDAO(),
                new OrderDAO()
        );
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

            if (path.equals("/deliveries/available") && method.equals("GET")) {
                handleGetAvailableOrders(exchange);
            } else {
                Utils.sendResponse(exchange, 404, gson.toJson(new ErrorResponseDto("Endpoint Not found")));
            }

        } catch (UserNotFoundException e) {
            Utils.sendResponse(exchange, 404, gson.toJson(new ErrorResponseDto(e.getMessage())));
        } catch (DeliveryServiceExceptions.UserNotCourier e){
            Utils.sendResponse(exchange, 403, gson.toJson(new ErrorResponseDto(e.getMessage())));
        } catch (RuntimeException e){
            Utils.sendResponse(exchange, 500, gson.toJson(new ErrorResponseDto("Internal Server Error")));
            System.err.println("Error in Delivery HTTPHandler");
            e.printStackTrace();
        }
    }


    private void handleGetAvailableOrders(HttpExchange exchange) throws IOException {

        String courierPhoneNumber = Utils.getAuthenticatedUserPhone(exchange);
        if (courierPhoneNumber == null) {
            return;
        }

        Utils.sendResponse(exchange, 200, gson.toJson(deliveryService.getAvailableOrders(courierPhoneNumber)));
    }
}
