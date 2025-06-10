package HTTPhandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dao.BuyerDAO;
import dao.FoodItemDAO;
import dao.RestaurantDAO;
import dao.UserDAO;
import dto.BuyerDto;
import dto.ErrorResponseDto;
import dto.FoodItemDto;
import dto.RestaurantDto;
import io.jsonwebtoken.io.IOException;
import service.BuyerService;
import service.RestaurantService;
import util.JwtUtil;
import util.RateLimiter;
import util.Utils;


import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class BuyerHTTPHandler implements HttpHandler {

    private final Gson gson = new GsonBuilder().serializeNulls().create();
    private final BuyerService buyerService;

    public BuyerHTTPHandler() {
        this.buyerService = new BuyerService(new UserDAO(), new RestaurantDAO(), new FoodItemDAO(), new BuyerDAO());
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException, java.io.IOException {
        String ip = exchange.getRemoteAddress().getAddress().getHostAddress();
        if (!RateLimiter.isAllowed(ip)) {
            Utils.sendResponse(exchange, 429, gson.toJson(new ErrorResponseDto("Too many requests")));
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        try {
            if (path.equals("/vendors") && "POST".equals(method)) {
                handleVendorsSearch(exchange);
            } else if (path.matches("/vendors/\\d+") && "GET".equals(method)) {
                Long id = Long.parseLong(path.split("/")[2]);
                handleItemList(exchange, id);
            } else if (path.equals("/items") && "POST".equals(method)) {
                handleSearchItems(exchange);
            } else if (path.matches("/items/\\d+") && "GET".equals(method)) {
                Long id = Long.parseLong(path.split("/")[2]);
                handleGetItem(exchange, id);
            }
        } catch (IllegalArgumentException e) {
            Utils.sendResponse(exchange, 400, gson.toJson(new ErrorResponseDto("Invalid input: " + e.getMessage())));
        } catch (com.google.gson.JsonSyntaxException e) {
            Utils.sendResponse(exchange, 400, gson.toJson(new ErrorResponseDto("Invalid JSON format: " + e.getMessage())));
        } catch (Exception e) {
            e.printStackTrace();
            Utils.sendResponse(exchange, 500, gson.toJson(new ErrorResponseDto("Internal server error.")));
        }
    }

    private void handleVendorsSearch(HttpExchange exchange) throws java.io.IOException {
        if (Utils.checkUnathorizedMediaType(exchange)) {
            Utils.sendResponse(exchange, 415, gson.toJson(new ErrorResponseDto("Unsupported media type")));
            return;
        }

        if (!Utils.isTokenValid(exchange)) {
            Utils.sendResponse(exchange, 401, gson.toJson(new ErrorResponseDto("Unauthorized request")));
        }

        BuyerDto.VendorSearch requestDto;
        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            requestDto = gson.fromJson(reader, BuyerDto.VendorSearch.class);

            if (requestDto == null) {
                Utils.sendResponse(exchange, 400, gson.toJson(new ErrorResponseDto("Invalid field name")));
                return;
            }
        }

        List<RestaurantDto.Response> list = buyerService.GetVendorsList(requestDto.getSearch(), requestDto.getKeywords());
        Utils.sendResponse(exchange, 200, gson.toJson(list));
    }

    private void handleItemList(HttpExchange exchange, Long restaurantId) throws java.io.IOException {
        if (!Utils.isTokenValid(exchange)) {
            Utils.sendResponse(exchange, 401, gson.toJson(new ErrorResponseDto("Unauthorized request")));
        }

        BuyerDto.ItemList itemListDto = buyerService.GetItemList(restaurantId);
        Utils.sendResponse(exchange, 200, gson.toJson(itemListDto));
    }

    private void handleSearchItems(HttpExchange exchange) throws java.io.IOException {
        if (Utils.checkUnathorizedMediaType(exchange)) {
            Utils.sendResponse(exchange, 415, gson.toJson(new ErrorResponseDto("Unsupported media type")));
            return;
        }

        if (!Utils.isTokenValid(exchange)) {
            Utils.sendResponse(exchange, 401, gson.toJson(new ErrorResponseDto("Unauthorized request")));
        }

        BuyerDto.ItemSearch requestDto;
        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            requestDto = gson.fromJson(reader, BuyerDto.ItemSearch.class);

            if (requestDto == null) {
                Utils.sendResponse(exchange, 400, gson.toJson(new ErrorResponseDto("Invalid field name")));
                return;
            }
        }

        List<FoodItemDto.Response> list = buyerService.GetItemsList(requestDto.getSearch(), requestDto.getPrice() ,requestDto.getKeywords());
        Utils.sendResponse(exchange, 200, gson.toJson(list));
    }

    private void handleGetItem(HttpExchange exchange, Long id) throws java.io.IOException {
        if (!Utils.isTokenValid(exchange)) {
            Utils.sendResponse(exchange, 401, gson.toJson(new ErrorResponseDto("Unauthorized request")));
        }

        FoodItemDto.Response itemDto = buyerService.GetItem(id);
        Utils.sendResponse(exchange, 200, gson.toJson(itemDto));

    }

}