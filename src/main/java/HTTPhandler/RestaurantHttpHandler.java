package HTTPhandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import dto.ErrorResponseDto;
import dto.RestaurantDto;
import service.RestaurantService;

import service.exception.UserNotFoundException;
import service.exception.RestaurantServiceExceptions;

import dao.UserDAO;
import dao.RestaurantDAO;

import util.JwtUtil;
import util.RateLimiter;
import util.Utils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class RestaurantHttpHandler implements HttpHandler {

    private final Gson gson = new GsonBuilder().serializeNulls().create();
    private final RestaurantService restaurantService;

    public RestaurantHttpHandler() {
        this.restaurantService = new RestaurantService(new UserDAO(), new RestaurantDAO());
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
            } else {
                Utils.sendResponse(exchange, 404, gson.toJson(new ErrorResponseDto("Resource not found")));
            }
        } catch (UserNotFoundException e) { // Catches the separate exception
            Utils.sendResponse(exchange, 404, gson.toJson(new ErrorResponseDto(e.getMessage())));
        }
        // Catch the specific exception for restaurant addition
        catch (RestaurantServiceExceptions.UserNotSeller e) {
            Utils.sendResponse(exchange, 403, gson.toJson(new ErrorResponseDto(e.getMessage())));
        } catch (RestaurantServiceExceptions.RestaurantAlreadyExists e) {
            Utils.sendResponse(exchange, 409, gson.toJson(new ErrorResponseDto(e.getMessage())));
        }
        catch (IllegalArgumentException e) {
            Utils.sendResponse(exchange, 400, gson.toJson(new ErrorResponseDto("Invalid input: " + e.getMessage())));
        } catch (com.google.gson.JsonSyntaxException e) {
            Utils.sendResponse(exchange, 400, gson.toJson(new ErrorResponseDto("Invalid JSON format: " + e.getMessage())));
        }
        catch (Exception e) {
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
        if (!Utils.isTokenValid(exchange)) {
            Utils.sendResponse(exchange, 401, gson.toJson(new ErrorResponseDto("Unauthorized request")));
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
        String ownerUserPhone = JwtUtil.validateToken(exchange.getRequestHeaders().getFirst("Authorization"));
        RestaurantDto.Response responseDto = restaurantService.createRestaurant(requestDto, ownerUserPhone);
        Utils.sendResponse(exchange, 201, gson.toJson(responseDto));
    }

    private void handleListRestaurants(HttpExchange exchange) throws IOException {

        // check for user token
        if (!Utils.isTokenValid(exchange)) {
            Utils.sendResponse(exchange, 401, gson.toJson(new ErrorResponseDto("Unauthorized request")));
            return;
        }

        String ownerUserPhone = JwtUtil.validateToken(exchange.getRequestHeaders().getFirst("Authorization"));
        Utils.sendResponse(exchange, 200, gson.toJson(restaurantService.listRestaurants(ownerUserPhone)));
    }
}