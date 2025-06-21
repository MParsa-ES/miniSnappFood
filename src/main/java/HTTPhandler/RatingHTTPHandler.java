package HTTPhandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dao.*;
import dto.BuyerDto;
import dto.ErrorResponseDto;
import dto.MessageDto;
import dto.RatingDTO;
import io.jsonwebtoken.io.IOException;
import service.BuyerService;
import service.RatingService;
import service.exception.OrderServiceExceptions;
import service.exception.UserNotFoundException;
import util.RateLimiter;
import util.Utils;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class RatingHTTPHandler implements HttpHandler {

    private final Gson gson = new GsonBuilder().serializeNulls().create();
    private final RatingService RatingService;

    public RatingHTTPHandler() {
        this.RatingService = new RatingService(new UserDAO(), new RestaurantDAO(), new FoodItemDAO(), new OrderDAO(), new RatingDAO());
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
            if (path.equals("/ratings") && "POST".equals(method)) {
                handleSubmitRating(exchange);
            } else if (path.matches("/vendors/\\d+") && "GET".equals(method)) {
                Long id = Long.parseLong(path.split("/")[2]);
                //handleItemList(exchange, id);
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

    private void handleSubmitRating(HttpExchange exchange) throws IOException, java.io.IOException {
//        if (Utils.checkUnathorizedMediaType(exchange)) {
//            Utils.sendResponse(exchange, 415, gson.toJson(new ErrorResponseDto("Unsupported media type")));
//            return;
//        }

        if (Utils.getAuthenticatedUserPhone(exchange) == null) {
            return;
        }

        RatingDTO.Request requestDto;
        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            requestDto = gson.fromJson(reader, RatingDTO.Request.class);

            if (requestDto == null) {
                Utils.sendResponse(exchange, 400, gson.toJson(new ErrorResponseDto("Invalid field name")));
                return;
            }
        }

        try {
            String phone = Utils.getAuthenticatedUserPhone(exchange);
            MessageDto response = RatingService.SubmitRating(requestDto, phone);
            Utils.sendResponse(exchange, 200, gson.toJson(response));
        } catch (OrderServiceExceptions | UserNotFoundException e) {
            Utils.sendResponse(exchange, 403, gson.toJson(new ErrorResponseDto(e.getMessage())));
        }

    }

}
