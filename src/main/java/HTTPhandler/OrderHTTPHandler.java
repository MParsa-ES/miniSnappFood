package HTTPhandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dao.FoodItemDAO;
import dao.OrderDAO;
import dao.RestaurantDAO;
import dao.UserDAO;
import dto.ErrorResponseDto;
import dto.OrderDto;

import service.OrderService;
import service.exception.OrderServiceExceptions;
import service.exception.RestaurantServiceExceptions;
import service.exception.UserNotFoundException;
import util.JwtUtil;
import util.RateLimiter;
import util.Utils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;


public class OrderHTTPHandler implements HttpHandler {


    private final Gson gson = new GsonBuilder().serializeNulls().create();
    private final OrderService orderService;

    public OrderHTTPHandler() {
        this.orderService = new OrderService(
                new UserDAO(),
                new RestaurantDAO(),
                new FoodItemDAO(),
                new OrderDAO()
        );
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        String ip = exchange.getRemoteAddress().getAddress().getHostAddress();
        if (!RateLimiter.isAllowed(ip)) {
            Utils.sendResponse(exchange, 429, gson.toJson(new ErrorResponseDto("Too many requests")));
            return;
        }


        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            if (path.equals("/orders") && method.equals("POST")) {
                handleCreateOrder(exchange);
            } else if (path.equals("/orders/history") && method.equals("GET")) {
                handleGetHistory(exchange);
            } else if (path.matches("/orders/\\d+") && method.equals("GET")) {
                Long id = Long.parseLong(path.split("/")[2]);
                handleGetOrderById(exchange, id);
            } else {
                Utils.sendResponse(exchange, 404, gson.toJson(new ErrorResponseDto("Order endpoint not found")));
            }
        } catch (UserNotFoundException | RestaurantServiceExceptions.RestaurantNotFound |
                 RestaurantServiceExceptions.ItemNotFound | OrderServiceExceptions.OrderNotFoundException e) {
            Utils.sendResponse(exchange, 404, gson.toJson(new ErrorResponseDto(e.getMessage())));
        } catch (OrderServiceExceptions.UserNotBuyerException | OrderServiceExceptions.NotOwnerOfOrderException e){
            Utils.sendResponse(exchange, 403, gson.toJson(new ErrorResponseDto(e.getMessage())));
        } catch (OrderServiceExceptions.ItemOutOfStockException e) {
            Utils.sendResponse(exchange, 409, gson.toJson(new ErrorResponseDto(e.getMessage())));
        } catch (IllegalArgumentException e) {
            Utils.sendResponse(exchange, 400, gson.toJson(new ErrorResponseDto(e.getMessage())));
        } catch (RuntimeException e){
            Utils.sendResponse(exchange, 500, gson.toJson(new ErrorResponseDto("Internal Server Error")));
            System.err.println("Error in OrderHTTPHandler");
            e.printStackTrace();
        }


    }

    private void handleCreateOrder(HttpExchange exchange) throws IOException {

        if (Utils.checkUnathorizedMediaType(exchange)) {
            Utils.sendResponse(exchange, 415, gson.toJson(new ErrorResponseDto("Unsupported media type")));
            return;
        }

        // check for user token
        if (!Utils.isTokenValid(exchange)) {
            Utils.sendResponse(exchange, 401, gson.toJson(new ErrorResponseDto("Unauthorized request")));
            return;
        }

        OrderDto.CreateRequest requestDto;

        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            requestDto = gson.fromJson(reader, OrderDto.CreateRequest.class);

            if (requestDto == null) {
                Utils.sendResponse(exchange, 400, gson.toJson(new ErrorResponseDto("Request body is missing")));
                return;
            }
        }

        String customerUserPhone = JwtUtil.validateToken(exchange.getRequestHeaders().getFirst("Authorization"));
        Utils.sendResponse(exchange, 200, gson.toJson(orderService.createOrder(requestDto, customerUserPhone)));

    }

    private void handleGetHistory(HttpExchange exchange) throws IOException {
        if (!Utils.isTokenValid(exchange)) {
            Utils.sendResponse(exchange, 401, gson.toJson(new ErrorResponseDto("Unauthorized request")));
            return;
        }

        String query = exchange.getRequestURI().getQuery();

        String vendor = null;
        String search = null;

        if (query != null) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length == 2) {
                    String key = pair[0];
                    String value = pair[1];

                    if (key.equals("vendor"))
                        vendor = value;
                    if (key.equals("search"))
                        search = value;
                }
            }
        }

        String customerUserPhone = JwtUtil.validateToken(exchange.getRequestHeaders().getFirst("Authorization"));
        Utils.sendResponse(exchange, 200, gson.toJson(orderService.getOrderHistory(customerUserPhone, vendor, search)));
    }

    private void handleGetOrderById(HttpExchange exchange, Long id) throws IOException {
        if (!Utils.isTokenValid(exchange)) {
            Utils.sendResponse(exchange, 401, gson.toJson(new ErrorResponseDto("Unauthorized request")));
            return;
        }

        String customerUserPhone = JwtUtil.validateToken(exchange.getRequestHeaders().getFirst("Authorization"));
        Utils.sendResponse(exchange, 200, gson.toJson(orderService.getOrderById(customerUserPhone, id)));

    }
}
