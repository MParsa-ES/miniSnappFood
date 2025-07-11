package HTTPhandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dao.*;
import dto.ErrorResponseDto;
import dto.MessageDto;
import dto.TransactionDTO;
import io.jsonwebtoken.io.IOException;
import service.TransactionService;
import service.exception.OrderServiceExceptions;
import service.exception.UserNotFoundException;
import util.RateLimiter;
import util.Utils;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

public class TransactionHTTPHandler implements HttpHandler {

    private final Gson gson = new GsonBuilder().serializeNulls().create();
    private final TransactionService transactionService;

    public TransactionHTTPHandler() {
        this.transactionService = new TransactionService(new UserDAO(), new RestaurantDAO(), new FoodItemDAO(), new OrderDAO(), new RatingDAO(), new TransactionDAO());
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
            if (path.equals("/payment/online") && "POST".equals(method)) {
                handlePayment(exchange);
            } else if (path.equals("/wallet/top-up") && "POST".equals(method)) {
                handleWalletTopUp(exchange);
            } else if (path.equals("/transactions") && "GET".equals(method)) {
                handleUserTransactions(exchange);
            } else if (path.equals("/admin/transactions") && "GET".equals(method)) {
                handleGetTransactions(exchange);
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

    private void handlePayment(HttpExchange exchange) throws IOException, java.io.IOException {
        if (Utils.checkUnathorizedMediaType(exchange)) {
            Utils.sendResponse(exchange, 415, gson.toJson(new ErrorResponseDto("Unsupported media type")));
            return;
        }

        if (Utils.getAuthenticatedUserPhone(exchange) == null) {
            Utils.sendResponse(exchange, 401, gson.toJson(new ErrorResponseDto("Unauthorized request")));
        }

        TransactionDTO.PaymentRequestDTO requestDTO;
        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            requestDTO = gson.fromJson(reader, TransactionDTO.PaymentRequestDTO.class);

            if (requestDTO == null) {
                Utils.sendResponse(exchange, 400, gson.toJson(new ErrorResponseDto("Invalid field name")));
                return;
            }
        }

        try {
            String phone = Utils.getAuthenticatedUserPhone(exchange);
            TransactionDTO.PaymentResponseDTO responseDTO = transactionService.payment(requestDTO, phone);
            Utils.sendResponse(exchange, 200, gson.toJson(responseDTO));
        } catch (OrderServiceExceptions.OrderNotCompleted e) {
            Utils.sendResponse(exchange, 400, gson.toJson(e));
        } catch (UserNotFoundException | OrderServiceExceptions.OrderNotFound e) {
            Utils.sendResponse(exchange, 404, gson.toJson(e));
        } catch (OrderServiceExceptions.InvalidOrderState e) {
            Utils.sendResponse(exchange, 409, gson.toJson(e));
        }

    }

    private void handleWalletTopUp(HttpExchange exchange) throws IOException, java.io.IOException {
        if (Utils.checkUnathorizedMediaType(exchange)) {
            Utils.sendResponse(exchange, 415, gson.toJson(new ErrorResponseDto("Unauthorized request")));
        }

        if (Utils.getAuthenticatedUserPhone(exchange) == null) {
            Utils.sendResponse(exchange, 401, gson.toJson(new ErrorResponseDto("Unauthorized request")));
        }

        TransactionDTO.TopUpRequestDTO requestDto;
        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            requestDto = gson.fromJson(reader, TransactionDTO.TopUpRequestDTO.class);

            if (requestDto == null || requestDto.getAmount() == null || requestDto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                Utils.sendResponse(exchange, 400, gson.toJson(new ErrorResponseDto("Invalid or missing amount")));
                return;
            }
        }

        try {
            String phone = Utils.getAuthenticatedUserPhone(exchange);
            MessageDto responseDTO = transactionService.TopUpWallet(requestDto.getAmount(), phone);
            Utils.sendResponse(exchange, 200, gson.toJson(responseDTO));
        } catch (UserNotFoundException e) {
            Utils.sendResponse(exchange, 404, gson.toJson(e));
        }

    }

    private void handleUserTransactions(HttpExchange exchange) throws IOException, java.io.IOException {
        if (Utils.getAuthenticatedUserPhone(exchange) == null) {
            Utils.sendResponse(exchange, 401, gson.toJson(new ErrorResponseDto("Unauthorized request")));
        }

        try {
            String phone = Utils.getAuthenticatedUserPhone(exchange);
            TransactionDTO.TransactionsList responseDTO = transactionService.getUserTransactions(phone);
            Utils.sendResponse(exchange, 200, gson.toJson(responseDTO));
        } catch (UserNotFoundException e) {
            Utils.sendResponse(exchange, 404, gson.toJson(e));
        }

    }

    private void handleGetTransactions(HttpExchange exchange) throws IOException, java.io.IOException {
        String adminUserName = Utils.getAuthenticatedUserPhone(exchange);
        if (adminUserName == null) {
            return;
        }

        String query = exchange.getRequestURI().getQuery();

        String search = null;
        String user = null;
        String method = null;
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
                            user = java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                            break;
                        case "courier":
                            method = java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                            break;
                        case "status":
                            status = java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                            break;
                    }
                }
            }
        }

        TransactionDTO.TransactionsList responseDTO = transactionService.searchTransactions(adminUserName, search, user, method, status);
        Utils.sendResponse(exchange, 200, gson.toJson(responseDTO));

    }

}
