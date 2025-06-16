package HTTPhandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dao.OrderDAO;
import dao.UserDAO;
import dto.ErrorResponseDto;
import dto.OrderDto;
import service.DeliveryService;
import service.exception.DeliveryServiceExceptions;
import service.exception.OrderServiceExceptions;
import service.exception.UserNotFoundException;
import util.RateLimiter;
import util.Utils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

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
            } else if (path.matches("^/deliveries/\\d+$") && method.equals("PATCH")) {
                Long orderId = Long.parseLong(path.split("/")[2]);
                handleChangeStatus(exchange, orderId);

            } else {
                Utils.sendResponse(exchange, 404, gson.toJson(new ErrorResponseDto("Endpoint Not found")));
            }

        } catch (UserNotFoundException | OrderServiceExceptions.OrderNotFound e) {
            Utils.sendResponse(exchange, 404, gson.toJson(new ErrorResponseDto(e.getMessage())));
        } catch (DeliveryServiceExceptions.UserNotCourier e) {
            Utils.sendResponse(exchange, 403, gson.toJson(new ErrorResponseDto(e.getMessage())));
        } catch (DeliveryServiceExceptions.OrderNotReadyForDelivery |
                 DeliveryServiceExceptions.OrderAlreadyAssignedToCourier | DeliveryServiceExceptions.CourierIsBusy e) {
            Utils.sendResponse(exchange, 409, gson.toJson(new ErrorResponseDto(e.getMessage())));
        } catch (RuntimeException e) {
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

    private void handleChangeStatus(HttpExchange exchange, Long orderId) throws IOException {

        if (Utils.checkUnathorizedMediaType(exchange)) {
            Utils.sendResponse(exchange, 415, gson.toJson(new ErrorResponseDto("Unsupported Media Type")));
            return;
        }

        String courierPhoneNumber = Utils.getAuthenticatedUserPhone(exchange);
        if (courierPhoneNumber == null) {
            return;
        }

        OrderDto.OrderStatusChangeRequest requestDto;
        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            requestDto = gson.fromJson(reader, OrderDto.OrderStatusChangeRequest.class);

            if (requestDto == null || requestDto.getStatus() == null || requestDto.getStatus().isBlank()) {
                Utils.sendResponse(exchange, 400, "Required filed 'status' is missing");
                return;
            }
        }

        Utils.sendResponse(exchange, 200, gson.toJson(deliveryService.updateOrderStatus(requestDto, courierPhoneNumber, orderId)));

    }
}
