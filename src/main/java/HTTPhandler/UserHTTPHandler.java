package HTTPhandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dto.*;
import entity.BankInfo;
import entity.Profile;
import entity.User;
import entity.Role;
import org.hibernate.Session;
import org.hibernate.Transaction;
import util.HibernateUtil;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class UserHTTPHandler implements HttpHandler {

    private final Gson gson = new GsonBuilder().serializeNulls().create();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {
            String path = exchange.getRequestURI().getPath();

            if (path.equals("/auth/register")) {
                handleRegister(exchange);
            } else if (path.equals("/auth/login")) {
                handleLogin(exchange);
            } else {
                sendResponse(exchange, 404, "{\"error\":\"Not found\"}");
            }
        } else {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
        }
    }

    private void handleRegister(HttpExchange exchange) throws IOException {
        InputStreamReader reader = new InputStreamReader(exchange.getRequestBody());
        UserRegisterRequestDto requestDto = new Gson().fromJson(reader, UserRegisterRequestDto.class);

        if (requestDto.getFull_name() == null || requestDto.getPhone() == null ||
                requestDto.getPassword() == null || requestDto.getRole() == null ||
                requestDto.getAddress() == null) {
            sendResponse(exchange, 400, "{\"error\":\"Invalid input\"}");
            return;
        }

        Role role;
        try {
            role = Role.valueOf(requestDto.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, 400, "{\"error\":\"Invalid input\"}");
            return;
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            if (isPhoneTaken(session, requestDto.getPhone())) {
                sendResponse(exchange, 409, "{\"error\":\"Phone number already exists\"}");
                return;
            }

            Transaction transaction = session.beginTransaction();

            User user = getUser(requestDto, role);


            session.save(user);

            transaction.commit();


            UserRegisterResponseDto responseDto = new UserRegisterResponseDto();
            responseDto.setMessage("User registered successfully");
            responseDto.setUser_id(user.getId().toString());
            responseDto.setToken("example-token"); // TODO: Implement real token generation

            String jsonResponse = gson.toJson(responseDto);
            sendResponse(exchange, 200, jsonResponse);

        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "{\"error\":\"Internal server error\"}");
        }
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        InputStreamReader reader = new InputStreamReader(exchange.getRequestBody());
        UserLoginRequestDto dto = new Gson().fromJson(reader, UserLoginRequestDto.class);

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            User user = session.createQuery("from User where phone = :phone", User.class)
                    .setParameter("phone", dto.getPhone())
                    .uniqueResult();

            if (user == null || !user.getPassword().equals(dto.getPassword())) {

                ErrorResponseDto error = new ErrorResponseDto("Invalid phone or password");
                String response = new Gson().toJson(error);
                exchange.sendResponseHeaders(401, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }


            UserLoginResponseDto responseDto = new UserLoginResponseDto();
            responseDto.setMessage("User logged in successfully");
            responseDto.setToken("example-token"); //TODO generate a token later

            UserLoginResponseDto.UserData userData = new UserLoginResponseDto.UserData();
            userData.setId(String.valueOf(user.getId()));
            userData.setFull_name(user.getFullName());
            userData.setPhone(user.getPhone());
            userData.setEmail(user.getEmail());
            userData.setRole(user.getRole().toString());
            userData.setAddress(user.getAddress());
            responseDto.setUser(userData);

            String json = new Gson().toJson(responseDto);
            exchange.sendResponseHeaders(200, json.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(json.getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
            ErrorResponseDto error = new ErrorResponseDto("Login failed due to server error");
            String response = new Gson().toJson(error);
            exchange.sendResponseHeaders(500, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }

    private User getUser(UserRegisterRequestDto requestDto, Role role) {
        User user = new User();
        user.setFullName(requestDto.getFull_name());
        user.setPhone(requestDto.getPhone());
        user.setEmail(requestDto.getEmail());
        user.setPassword(requestDto.getPassword());
        user.setRole(role);
        user.setAddress(requestDto.getAddress());


        BankInfo bankInfo = null;
        if (requestDto.getBank_info() != null) {
            bankInfo = new BankInfo();
            bankInfo.setBankName(requestDto.getBank_info().getBank_name());
            bankInfo.setAccountNumber(requestDto.getBank_info().getAccount_number());

        }


        Profile profile = new Profile();
        profile.setProfilePicture(requestDto.getProfileImageBase64());
        profile.setBankInfo(bankInfo);


        if (bankInfo != null) {
            bankInfo.setProfile(profile);
        }
        profile.setUser(user);
        user.setProfile(profile);
        return user;
    }

    private boolean isPhoneTaken(Session session, String phone) {
        User existing = session.createQuery("from User where phone = :phone", User.class)
                .setParameter("phone", phone)
                .uniqueResult();
        return existing != null;
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String responseBody) throws IOException {
        byte[] responseBodyBytes = responseBody.getBytes();
        exchange.sendResponseHeaders(statusCode, responseBodyBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBodyBytes);
        }
    }

}
