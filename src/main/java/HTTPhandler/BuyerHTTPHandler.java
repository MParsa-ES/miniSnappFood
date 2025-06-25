package HTTPhandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dao.*;
import dto.*;
import io.jsonwebtoken.io.IOException;
import service.BuyerService;
import service.exception.CouponServiceExceptions;
import service.exception.OrderServiceExceptions;
import service.exception.RestaurantServiceExceptions;
import service.exception.UserNotFoundException;
import util.LocalDateAdapter;
import util.RateLimiter;
import util.Utils;


import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

public class BuyerHTTPHandler implements HttpHandler {

    private final Gson gson = new GsonBuilder().registerTypeAdapter(LocalDate.class, new LocalDateAdapter()).serializeNulls().create();
    private final BuyerService buyerService;

    public BuyerHTTPHandler() {
        this.buyerService = new BuyerService(new UserDAO(), new RestaurantDAO(), new FoodItemDAO(), new BuyerDAO(), new CouponDAO());
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
            } else if (path.equals("/favorites") && "GET".equals(method)) {
                handleGetFavorites(exchange);
            } else if (path.matches("/favorites/\\d+") && "POST".equals(method)) {
                Long id = Long.parseLong(path.split("/")[2]);
                handleAddFavorite(exchange, id);
            } else if (path.matches("/favorites/\\d+") && "DELETE".equals(method)) {
                Long id = Long.parseLong(path.split("/")[2]);
                handleRemoveFavorite(exchange, id);
            } else if (path.equals("/coupons") && "GET".equals(method)) {
                handleCheckCoupon(exchange);

            } else {
                Utils.sendResponse(exchange, 404, gson.toJson(new ErrorResponseDto("Buyer endpoint not found.")));
            }
        } catch (CouponServiceExceptions.CouponNotFound | UserNotFoundException |
                 RestaurantServiceExceptions.RestaurantNotFound | RestaurantServiceExceptions.ItemNotFound e) {
            Utils.sendResponse(exchange, 404, gson.toJson(new ErrorResponseDto(e.getMessage())));
        } catch (CouponServiceExceptions.InvalidCoupon | OrderServiceExceptions.UserNotBuyer e) {
            Utils.sendResponse(exchange, 403, gson.toJson(new ErrorResponseDto(e.getMessage())));
        } catch (RestaurantServiceExceptions.RestaurantAlreadyFavorite |
                 RestaurantServiceExceptions.RestaurantAlreadyExists e) {
            Utils.sendResponse(exchange, 409, gson.toJson(new ErrorResponseDto(e.getMessage())));
        } catch (IllegalArgumentException e) {
            Utils.sendResponse(exchange, 400, gson.toJson(new ErrorResponseDto(e.getMessage())));
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

        if (Utils.getAuthenticatedUserPhone(exchange) == null) {
            return;
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
        if (Utils.getAuthenticatedUserPhone(exchange) == null) {
            return;
        }

        BuyerDto.ItemList itemListDto = buyerService.GetItemList(restaurantId);
        Utils.sendResponse(exchange, 200, gson.toJson(itemListDto));
    }

    private void handleSearchItems(HttpExchange exchange) throws java.io.IOException {
        if (Utils.checkUnathorizedMediaType(exchange)) {
            Utils.sendResponse(exchange, 415, gson.toJson(new ErrorResponseDto("Unsupported media type")));
            return;
        }

        if (Utils.getAuthenticatedUserPhone(exchange) == null) {
            return;
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
        if (Utils.getAuthenticatedUserPhone(exchange) == null) {
            return;
        }

        FoodItemDto.Response itemDto = buyerService.GetItem(id);
        Utils.sendResponse(exchange, 200, gson.toJson(itemDto));

    }

    private void handleGetFavorites(HttpExchange exchange) throws java.io.IOException {
        String phone = Utils.getAuthenticatedUserPhone(exchange);
        if (phone == null) {
            return;
        }

        List<RestaurantDto.Response> list = buyerService.getFavoriteRestaurants(phone);
        Utils.sendResponse(exchange, 200, gson.toJson(list));

    }

    private void handleAddFavorite(HttpExchange exchange, Long restaurantId) throws java.io.IOException {
        String phone = Utils.getAuthenticatedUserPhone(exchange);
        if (phone == null) {
            return;
        }

        MessageDto messageDto = buyerService.addFavoriteRestaurant(restaurantId, phone);
        Utils.sendResponse(exchange, 200, gson.toJson(messageDto));

    }

    private void handleRemoveFavorite(HttpExchange exchange, Long restaurantId) throws java.io.IOException {
        String phone = Utils.getAuthenticatedUserPhone(exchange);
        if (phone == null) {
            return;
        }

        MessageDto messageDto = buyerService.removeFavoriteRestaurant(restaurantId, phone);
        Utils.sendResponse(exchange, 200, gson.toJson(messageDto));

    }

    private void handleCheckCoupon(HttpExchange exchange) throws java.io.IOException {

        String customerUserPhone = Utils.getAuthenticatedUserPhone(exchange);

        if (customerUserPhone == null) {
            return;
        }

        String query = exchange.getRequestURI().getQuery();
        String couponCode = null;

        if (query != null) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=", 2);

                if (pair.length > 1 && pair[0].equals("coupon_code")) {
                    couponCode = java.net.URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                    break;
                }
            }
        }

        if (couponCode == null || couponCode.isBlank()) {
            throw new IllegalArgumentException("Required parameter 'coupon_code' is missing");
        }

        Utils.sendResponse(exchange, 200, gson.toJson(buyerService.checkCoupon(customerUserPhone, couponCode)));

    }

}