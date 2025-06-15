package HTTPhandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import dao.*;
import dto.*;
import service.FoodItemService;
import service.MenuService;
import service.OrderService;
import service.RestaurantService;

import service.exception.MenuServiceExceptions;
import service.exception.OrderServiceExceptions;
import service.exception.UserNotFoundException;
import service.exception.RestaurantServiceExceptions;

import util.RateLimiter;
import util.Utils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class RestaurantHttpHandler implements HttpHandler {

    private final Gson gson = new GsonBuilder().serializeNulls().create();
    private final RestaurantService restaurantService;
    private final FoodItemService foodItemService;
    private final MenuService menuService;
    private final OrderService orderService;

    public RestaurantHttpHandler() {
        this.restaurantService = new RestaurantService(new UserDAO(), new RestaurantDAO());
        this.foodItemService = new FoodItemService(new UserDAO(), new RestaurantDAO(), new FoodItemDAO());
        this.menuService = new MenuService(new UserDAO(), new MenuDAO(), new RestaurantDAO(), new FoodItemDAO());
        this.orderService = new OrderService(new UserDAO(), new RestaurantDAO(), new FoodItemDAO(), new OrderDAO());
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
            if ("/restaurants".equals(path) && "POST".equals(method)) {
                handleCreateRestaurant(exchange);


            } else if ("/restaurants/mine".equals(path) && "GET".equals(method)) {
                handleListRestaurants(exchange);


            } else if (path.matches("^/restaurants/\\d+$") && "PUT".equals(method)) {
                Long id = Long.parseLong(path.split("/")[2]);
                handleUpdateRestaurant(exchange, id);


            } else if (path.matches("/restaurants/\\d+/item") && "POST".equals(method)) {
                handleAddItem(exchange);


            } else if (path.matches("/restaurants/\\d+/item/\\d+") && "GET".equals(method)) {
                Long restaurantId = Long.parseLong(path.split("/")[2]);
                Long itemId = Long.parseLong(path.split("/")[4]);
                handleUpdateItem(exchange, restaurantId, itemId);


            } else if (path.matches("/restaurants/\\d+/item/\\d+") && "DELETE".equals(method)) {
                Long restaurantId = Long.parseLong(path.split("/")[2]);
                Long itemId = Long.parseLong(path.split("/")[4]);
                handleDeleteItem(exchange, restaurantId, itemId);


            } else if (path.matches("^/restaurants/\\d+/menu$") && "POST".equals(method)) {
                Long id = Long.parseLong(path.split("/")[2]);
                handleAddMenu(exchange, id);


            } else if (path.matches("^/restaurants/\\d+/menu/[^/]+$") && "DELETE".equals(method)) {
                Long id = Long.parseLong(path.split("/")[2]);
                String title = path.split("/")[4];
                handleDeleteMenu(exchange, id, title);


            } else if (path.matches("^/restaurants/\\d+/menu/[^/]+$") && "PUT".equals(method)) {
                Long id = Long.parseLong(path.split("/")[2]);
                String title = path.split("/")[4];
                handleAddFoodToMenu(exchange, id, title);

            } else if (path.matches("^/restaurants/\\d+/menu/[^/]+/\\d+$")) {
                Long restaurantId = Long.parseLong(path.split("/")[2]);
                String title = path.split("/")[4];
                Long foodId = Long.parseLong(path.split("/")[5]);
                handleDeleteFoodFromMenu(exchange, restaurantId, title, foodId);


            } else if (path.matches("^/restaurants/\\d+/orders$") && "GET".equals(method)) {
                Long restaurantId = Long.parseLong(path.split("/")[2]);
                handleGetRestaurantOrders(exchange , restaurantId);


            } else if (path.matches("^/restaurants/orders/\\d+$") && "PATCH".equals(method)) {
                Long orderId = Long.parseLong(path.split("/")[3]);
                handleUpdateOrderStatus(exchange, orderId);


            } else {
                Utils.sendResponse(exchange, 404, gson.toJson(new ErrorResponseDto("Resource not found")));
            }
        } catch (UserNotFoundException | RestaurantServiceExceptions.RestaurantNotFound |
                 MenuServiceExceptions.MenuNotFoundException | RestaurantServiceExceptions.ItemNotFound |
                 MenuServiceExceptions.FoodNotInMenu | OrderServiceExceptions.OrderNotFound e) { // Catches the separate exception
            Utils.sendResponse(exchange, 404, gson.toJson(new ErrorResponseDto(e.getMessage())));
        }
        // Catch the specific exception for restaurant addition
        catch (RestaurantServiceExceptions.UserNotSeller |
               RestaurantServiceExceptions.UserNotOwner e) {
            Utils.sendResponse(exchange, 403, gson.toJson(new ErrorResponseDto(e.getMessage())));
        } catch (RestaurantServiceExceptions.RestaurantAlreadyExists |
                 RestaurantServiceExceptions.ItemAlreadyExists |
                 MenuServiceExceptions.MenuIsDuplicateException |
                 OrderServiceExceptions.RestaurantNotOwnerOfOrder e) {
            Utils.sendResponse(exchange, 409, gson.toJson(new ErrorResponseDto(e.getMessage())));
        } catch (IllegalArgumentException e) {
            Utils.sendResponse(exchange, 400, gson.toJson(new ErrorResponseDto("Invalid input: " + e.getMessage())));
        } catch (com.google.gson.JsonSyntaxException e) {
            Utils.sendResponse(exchange, 400, gson.toJson(new ErrorResponseDto("Invalid JSON format: " + e.getMessage())));
        } catch (Exception e) {
            e.printStackTrace();
            Utils.sendResponse(exchange, 500, gson.toJson(new ErrorResponseDto("Internal server error.")));
        }
    }

    private void handleCreateRestaurant(HttpExchange exchange) throws IOException {
        if (Utils.checkUnathorizedMediaType(exchange)) {
            Utils.sendResponse(exchange, 415, gson.toJson(new ErrorResponseDto("Unsupported media type")));
            return;
        }

        // check for user token
        String ownerUserPhone = Utils.getAuthenticatedUserPhone(exchange);
        if (ownerUserPhone == null) {
            return;
        }

        RestaurantDto.Request requestDto;
        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            requestDto = gson.fromJson(reader, RestaurantDto.Request.class);

            // body of json request is empty
            if (requestDto == null) {
                Utils.sendResponse(exchange, 400, gson.toJson(new ErrorResponseDto("Invalid field name")));
                return;
            }
        }

        // required fields missing
        if (requestDto.getName() == null || requestDto.getName().isBlank() ||
                requestDto.getAddress() == null || requestDto.getAddress().isBlank() ||
                requestDto.getPhone() == null || requestDto.getPhone().isBlank()) {
            Utils.sendResponse(exchange, 400, gson.toJson(new ErrorResponseDto("Invalid field name")));
            return;
        }

        // Exceptions from here will be caught by the main handle() method's try-catch blocks
        RestaurantDto.Response responseDto = restaurantService.createRestaurant(requestDto, ownerUserPhone);
        Utils.sendResponse(exchange, 201, gson.toJson(responseDto));
    }

    private void handleListRestaurants(HttpExchange exchange) throws IOException {

        // check for user token
        String ownerUserPhone = Utils.getAuthenticatedUserPhone(exchange);
        if (ownerUserPhone == null) {
            return;
        }

        Utils.sendResponse(exchange, 200, gson.toJson(restaurantService.listRestaurants(ownerUserPhone)));
    }

    private void handleUpdateRestaurant(HttpExchange exchange, Long id) throws IOException {
        if (Utils.checkUnathorizedMediaType(exchange)) {
            Utils.sendResponse(exchange, 415, gson.toJson(new ErrorResponseDto("Unsupported media type")));
            return;
        }

        String ownerUserPhone = Utils.getAuthenticatedUserPhone(exchange);
        if (ownerUserPhone == null) {
            return;
        }

        RestaurantDto.Request requestDto;
        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            requestDto = gson.fromJson(reader, RestaurantDto.Request.class);

            if (requestDto == null) {
                Utils.sendResponse(exchange, 400, gson.toJson(new ErrorResponseDto("Invalid field name")));
                return;
            }
        }

        Utils.sendResponse(exchange, 200, gson.toJson(restaurantService.updateRestaurant(requestDto, ownerUserPhone, id)));

    }

    private void handleAddItem(HttpExchange exchange) throws IOException {
        if (Utils.checkUnathorizedMediaType(exchange)) {
            Utils.sendResponse(exchange, 415, gson.toJson(new ErrorResponseDto("Unsupported media type")));
            return;
        }

        String ownerUserPhone = Utils.getAuthenticatedUserPhone(exchange);
        if (ownerUserPhone == null) {
            return;
        }

        FoodItemDto.Request requestDto;
        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            requestDto = gson.fromJson(reader, FoodItemDto.Request.class);

            // body of json request is empty
            if (requestDto == null) {
                Utils.sendResponse(exchange, 400, gson.toJson(new ErrorResponseDto("Invalid field name")));
                return;
            }
        }

        // required fields missing
        if (requestDto.getName() == null || requestDto.getName().isBlank() ||
                requestDto.getDescription() == null || requestDto.getKeywords().isEmpty()) {
            Utils.sendResponse(exchange, 400, gson.toJson(new ErrorResponseDto("Invalid field name")));
            return;
        }

        FoodItemDto.Response responseDto = foodItemService.createFoodItem(requestDto, ownerUserPhone);
        Utils.sendResponse(exchange, 201, gson.toJson(responseDto));
    }

    private void handleUpdateItem(HttpExchange exchange, Long restaurantId, Long itemId) throws IOException {
        if (Utils.checkUnathorizedMediaType(exchange)) {
            Utils.sendResponse(exchange, 415, gson.toJson(new ErrorResponseDto("Unsupported media type")));
            return;
        }

        String ownerUserPhone = Utils.getAuthenticatedUserPhone(exchange);
        if (ownerUserPhone == null) {
            return;
        }

        FoodItemDto.Request requestDto;
        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            requestDto = gson.fromJson(reader, FoodItemDto.Request.class);

            if (requestDto == null) {
                Utils.sendResponse(exchange, 400, gson.toJson(new ErrorResponseDto("Invalid field name")));
                return;
            }
        }

        Utils.sendResponse(exchange, 200, gson.toJson(foodItemService.UpdateItem(requestDto, ownerUserPhone, restaurantId, itemId)));

    }

    private void handleDeleteItem(HttpExchange exchange, Long restaurantId, Long itemId) throws IOException {
        String ownerUserPhone = Utils.getAuthenticatedUserPhone(exchange);
        if (ownerUserPhone == null) {
            return;
        }

        Utils.sendResponse(exchange, 200, gson.toJson(foodItemService.DeleteItem(restaurantId, itemId)));
    }

    private void handleAddMenu(HttpExchange exchange, Long id) throws IOException {
        if (Utils.checkUnathorizedMediaType(exchange)) {
            Utils.sendResponse(exchange, 415, gson.toJson(new ErrorResponseDto("Unsupported media type")));
            return;
        }

        String ownerUserPhone = Utils.getAuthenticatedUserPhone(exchange);
        if (ownerUserPhone == null) {
            return;
        }

        MenuDto.Request requestDto;
        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            requestDto = gson.fromJson(reader, MenuDto.Request.class);
            if (requestDto.getTitle() == null) {
                Utils.sendResponse(exchange, 400, gson.toJson(new ErrorResponseDto("Invalid field name")));
                return;
            }
        }

        Utils.sendResponse(exchange, 200, gson.toJson(menuService.createMenu(requestDto, ownerUserPhone, id)));
    }

    private void handleDeleteMenu(HttpExchange exchange, Long id, String title) throws IOException {
        String ownerUserPhone = Utils.getAuthenticatedUserPhone(exchange);
        if (ownerUserPhone == null) {
            return;
        }

        Utils.sendResponse(exchange, 200, gson.toJson(menuService.deleteMenu(ownerUserPhone, id, title)));

    }

    private void handleAddFoodToMenu(HttpExchange exchange, Long restaurantId, String menuTitle) throws IOException {
        if (Utils.checkUnathorizedMediaType(exchange)) {
            Utils.sendResponse(exchange, 415, gson.toJson(new ErrorResponseDto("Unsupported media type")));
            return;
        }

        String ownerUserPhone = Utils.getAuthenticatedUserPhone(exchange);
        if (ownerUserPhone == null) {
            return;
        }

        MenuDto.AddItemRequest requestDto;
        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            requestDto = gson.fromJson(reader, MenuDto.AddItemRequest.class);
            if (requestDto == null) {
                Utils.sendResponse(exchange, 400, gson.toJson(new ErrorResponseDto("Invalid field name")));
                return;
            }
        }

        if (requestDto.getItem_id() == null) {
            Utils.sendResponse(exchange, 400, gson.toJson(new ErrorResponseDto("Invalid field name")));
        }

        Utils.sendResponse(exchange, 200, gson.toJson(
                menuService.addFoodToMenu(requestDto, ownerUserPhone, restaurantId, menuTitle)));

    }

    private void handleDeleteFoodFromMenu(HttpExchange exchange, Long restaurantId, String menuTitle, Long foodId) throws IOException {
        String ownerUserPhone = Utils.getAuthenticatedUserPhone(exchange);
        if (ownerUserPhone == null) {
            return;
        }

        Utils.sendResponse(exchange, 200, gson.toJson(menuService.deleteFoodFromMenu(ownerUserPhone, restaurantId, menuTitle, foodId)));
    }

    private void handleGetRestaurantOrders(HttpExchange exchange, Long restaurantId) throws IOException {

        String ownerUserPhone = Utils.getAuthenticatedUserPhone(exchange);
        if (ownerUserPhone == null) {
            return;
        }
        String query = exchange.getRequestURI().getQuery();

        String status = null;
        String search = null;
        String user = null;
        String courier = null;

        if (query != null) {
            for (String params : query.split("&")) {
                String[] pair = params.split("=", 2);
                if (pair.length == 2) {
                    if (pair[0].equals("status")) {
                        status = java.net.URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                    }

                    if (pair[0].equals("search")) {
                        search = java.net.URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                    }

                    if (pair[0].equals("user")) {
                        user = java.net.URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                    }

                    if (pair[0].equals("courier")) {
                        courier = java.net.URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                    }
                }
            }
        }


        Utils.sendResponse(exchange, 200,
                gson.toJson(orderService.getRestaurantOrders
                        (ownerUserPhone, restaurantId, status, search, user, courier)));

    }

    private void handleUpdateOrderStatus(HttpExchange exchange, Long orderId) throws IOException {

        if (Utils.checkUnathorizedMediaType(exchange)) {
            Utils.sendResponse(exchange, 415, gson.toJson(new ErrorResponseDto("Unsupported media type")));
            return;
        }

        String ownerUserPhone = Utils.getAuthenticatedUserPhone(exchange);
        if (ownerUserPhone == null) {
            return;
        }

        OrderDto.OrderStatusChangeRequest requestDto = null;

        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            requestDto = gson.fromJson(reader, OrderDto.OrderStatusChangeRequest.class);
            if (requestDto == null || requestDto.getStatus() == null || requestDto.getStatus().isBlank()) {
                Utils.sendResponse(exchange, 400, gson.toJson(new ErrorResponseDto("Required field status is missing")));
                return;
            }
        }

        Utils.sendResponse(exchange, 200, gson.toJson(orderService.updateOrderStatus(requestDto, ownerUserPhone, orderId)));
    }

}