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
import util.JwtUtil;

import util.RateLimiter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;


public class UserHTTPHandler implements HttpHandler {

    private final Gson gson = new GsonBuilder().serializeNulls().create();

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        // Checking correct Header for content type
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.toLowerCase().contains("application/json")) {
            sendResponse(exchange, 415, "{\n\"error\":\"Unsupported media type\"\n}");
            return;
        }

        // Checking for Bot
        String ip = exchange.getRemoteAddress().getAddress().getHostAddress();
        if (!RateLimiter.isAllowed(ip)){
            sendResponse(exchange, 429, "{\n\"error\":\"Too many requests\"\n}");
            return;
        }


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
                requestDto.getAddress() == null || (requestDto.getEmail() != null && !isValidEmail(requestDto.getEmail()))) {
            sendResponse(exchange, 400, "{\n\"error\":\"Invalid `field name`\"\n}");
            return;
        }

        Role role;
        try {
            role = Role.valueOf(requestDto.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            sendResponse(exchange, 400, "{\n\"error\":\"Invalid `field name`\"\n}");
            return;
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            if (isPhoneTaken(session, requestDto.getPhone())) {
                sendResponse(exchange, 409, "{\"error\":\"Phone number already exists\"}");
                return;
            }

            Transaction transaction = session.beginTransaction();

            // TODO check if the profileImage64 field address is invalid and return 404 not found code for it with "{\n\"error\":\"Resource not found\"\n}" message

            // only sellers and buyers can have bank info
            if (role == Role.BUYER && requestDto.getBank_info() != null) {
                sendResponse(exchange, 403, "{\n\"error\":\"Forbidden request\"\n}");
                return;
            }

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
        UserLoginRequestDto requestDto = new Gson().fromJson(reader, UserLoginRequestDto.class);

        if (requestDto.getPhone() == null || requestDto.getPassword() == null) {
            sendResponse(exchange, 400, "{\n\"error\":\"Invalid `field name`\"\n}");
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            User user = session.createQuery("from User where phone = :phone", User.class)
                    .setParameter("phone", requestDto.getPhone())
                    .uniqueResult();

            if (user == null || !user.getPassword().equals(requestDto.getPassword())) {

                ErrorResponseDto error = new ErrorResponseDto("Unauthorized request");
                String response = gson.toJson(error);
                exchange.sendResponseHeaders(401, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                return;
            }

            String token = JwtUtil.generateToken(user.getPhone());

            UserLoginResponseDto responseDto = new UserLoginResponseDto();
            responseDto.setMessage("User logged in successfully");
            responseDto.setToken(token);

            UserLoginResponseDto.UserData userData = new UserLoginResponseDto.UserData();
            userData.setId(String.valueOf(user.getId()));
            userData.setFull_name(user.getFullName());
            userData.setPhone(user.getPhone());
            userData.setEmail(user.getEmail());
            userData.setRole(user.getRole().toString());
            userData.setAddress(user.getAddress());


            // Setting the profile image and bank info of the login response requestDto
            if (user.getProfile() != null) {
                userData.setProfileImageBase64(user.getProfile().getProfilePicture());

                if (user.getProfile().getBankInfo() != null){
                    UserLoginResponseDto.UserData.BankInfoDto bankInfoDto = new UserLoginResponseDto.UserData.BankInfoDto();
                    bankInfoDto.setBank_name(user.getProfile().getBankInfo().getBankName());
                    bankInfoDto.setAccount_number(user.getProfile().getBankInfo().getAccountNumber());
                    userData.setBank_info(bankInfoDto);
                }
            }

            responseDto.setUser(userData);

            String response = gson.toJson(responseDto);
            exchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
            ErrorResponseDto error = new ErrorResponseDto("Internal server error");
            String response = gson.toJson(error);
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

    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return email.matches(emailRegex);
    }

}
