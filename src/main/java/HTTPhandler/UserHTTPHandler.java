package HTTPhandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import at.favre.lib.crypto.bcrypt.BCrypt;
import dto.*;
import entity.BankInfo;
import entity.Profile;
import entity.User;
import entity.Role;
import entity.InvalidToken;
import org.hibernate.Session;
import org.hibernate.Transaction;
import util.HibernateUtil;
import util.JwtUtil;
import util.RateLimiter;
import util.Utils;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;


public class UserHTTPHandler implements HttpHandler {

    private final Gson gson = new GsonBuilder().serializeNulls().create();

    @Override
    public void handle(HttpExchange exchange) throws IOException {


        // Checking for Bot
        String ip = exchange.getRemoteAddress().getAddress().getHostAddress();
        if (!RateLimiter.isAllowed(ip)) {
            Utils.sendResponse(exchange, 429, "{\n\"error\":\"Too many requests\"\n}");
            return;
        }


        if ("POST".equals(exchange.getRequestMethod())) {
            String path = exchange.getRequestURI().getPath();

            switch (path) {
                case "/auth/register" -> handleRegister(exchange);
                case "/auth/login" -> handleLogin(exchange);
                case "/auth/logout" -> handleLogout(exchange);
                default -> Utils.sendResponse(exchange, 404, "{\"error\":\"Not found\"}");
            }
        } else {
            Utils.sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
        }
    }

    private void handleRegister(HttpExchange exchange) throws IOException {

        // Checking correct Header for content type
        if (Utils.checkUnathorizedMediaType(exchange)) return;

        InputStreamReader reader = new InputStreamReader(exchange.getRequestBody());
        UserRegisterDto.Request requestDto = new Gson().fromJson(reader, UserRegisterDto.Request.class);

        if (requestDto.getFull_name() == null || requestDto.getPhone() == null ||
                requestDto.getPassword() == null || requestDto.getRole() == null ||
                requestDto.getAddress() == null || (requestDto.getEmail() != null && !Utils.isValidEmail(requestDto.getEmail()))) {
            Utils.sendResponse(exchange, 400, "{\n\"error\":\"Invalid field name\"\n}");
            return;
        }

        Role role;
        try {
            role = Role.valueOf(requestDto.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            Utils.sendResponse(exchange, 400, "{\n\"error\":\"Invalid field name\"\n}");
            return;
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            if (isPhoneTaken(session, requestDto.getPhone())) {
                Utils.sendResponse(exchange, 409, "{\"error\":\"Phone number already exists\"}");
                return;
            }

            Transaction transaction = session.beginTransaction();

            // TODO check if the profileImage64 field address is invalid and return 404 not found code for it with "{\n\"error\":\"Resource not found\"\n}" message

            // Only sellers and buyers can have bank info
            if (role == Role.BUYER && requestDto.getBank_info() != null) {
                Utils.sendResponse(exchange, 403, "{\n\"error\":\"Forbidden request\"\n}");
                return;
            }

            User user = getUser(requestDto, role);

            session.save(user);

            transaction.commit();

            String token = JwtUtil.generateToken(user.getPhone());

            UserRegisterDto.Response responseDto = new UserRegisterDto.Response();
            responseDto.setMessage("User registered successfully");
            responseDto.setUser_id(user.getId().toString());
            responseDto.setToken(token);

            String jsonResponse = gson.toJson(responseDto);
            Utils.sendResponse(exchange, 200, jsonResponse);

        } catch (Exception e) {
            e.printStackTrace();
            Utils.sendResponse(exchange, 500, "{\"error\":\"Internal server error\"}");
        }
    }

    private void handleLogin(HttpExchange exchange) throws IOException {

        // Checking correct Header for content type
        if (Utils.checkUnathorizedMediaType(exchange)) return;

        InputStreamReader reader = new InputStreamReader(exchange.getRequestBody());
        UserLoginDto.Request requestDto = new Gson().fromJson(reader, UserLoginDto.Request.class);

        if (requestDto.getPhone() == null || requestDto.getPassword() == null) {
            Utils.sendResponse(exchange, 400, "{\n\"error\":\"Invalid `field name`\"\n}");
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            User user = session.createQuery("from User where phone = :phone", User.class)
                    .setParameter("phone", requestDto.getPhone())
                    .uniqueResult();

            if (user == null) {
                Utils.sendResponse(exchange, 401, gson.toJson("{\n\"error\":\"Unauthorized request\"\n}"));
                return;
            }

            String rawPassword = requestDto.getPassword();
            String hashedPasswordFromDb = user.getPassword();
            BCrypt.Result result = BCrypt.verifyer().verify(rawPassword.toCharArray(), hashedPasswordFromDb);

            if (!result.verified) {
                Utils.sendResponse(exchange, 401, gson.toJson("{\n\"error\":\"Unauthorized request\"\n}"));
                return;
            }

            String token = JwtUtil.generateToken(user.getPhone());

            UserLoginDto.Response responseDto = new UserLoginDto.Response();
            responseDto.setMessage("User logged in successfully");
            responseDto.setToken(token);

            UserLoginDto.Response.UserData userData = new UserLoginDto.Response.UserData();
            userData.setId(String.valueOf(user.getId()));
            userData.setFull_name(user.getFullName());
            userData.setPhone(user.getPhone());
            userData.setEmail(user.getEmail());
            userData.setRole(user.getRole().toString());
            userData.setAddress(user.getAddress());


            // Setting the profile image and bank info of the login response requestDto
            if (user.getProfile() != null) {
                userData.setProfileImageBase64(user.getProfile().getProfileImageBase64());

                if (user.getProfile().getBank_info() != null) {
                    UserLoginDto.Response.UserData.BankInfoDto bankInfoDto = new UserLoginDto.Response.UserData.BankInfoDto();
                    bankInfoDto.setBank_name(user.getProfile().getBank_info().getBankName());
                    bankInfoDto.setAccount_number(user.getProfile().getBank_info().getAccountNumber());
                    userData.setBank_info(bankInfoDto);
                }
            }

            responseDto.setUser(userData);
            Utils.sendResponse(exchange, 200, gson.toJson(responseDto));
        } catch (Exception e) {
            e.printStackTrace();
            Utils.sendResponse(exchange, 500, "{\"error\":\"Internal server error\"}");
        }
    }

    private void handleLogout(HttpExchange exchange) throws IOException {

        String token = exchange.getRequestHeaders().getFirst("Authorization");

        // Check if token exists
        if (token == null || token.isEmpty()) {
            Utils.sendResponse(exchange, 401, gson.toJson("{\n\"error\":\"Unauthorized request\"\n}"));
            return;
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            if (JwtUtil.validateToken(token) == null) {
                Utils.sendResponse(exchange, 401, gson.toJson("{\n\"error\":\"Unauthorized request\"\n}"));
                return;
            }

            String jti = JwtUtil.getJtiFromToken(token);
            Date expiryDate = JwtUtil.getExpirationDateFromToken(token);

            if (jti != null && expiryDate != null) {
                Transaction transaction = session.beginTransaction();
                InvalidToken invalidToken = new InvalidToken(jti, expiryDate);
                session.save(invalidToken);
                transaction.commit();
            }

            Utils.sendResponse(exchange, 200, "{\"message\":\"User Logged out successfully\"}");

        } catch (Exception e) {
            e.printStackTrace();
            Utils.sendResponse(exchange, 500, "{\"error\":\"Internal server error\"}");
        }
    }

    private User getUser(UserRegisterDto.Request requestDto, Role role) {
        User user = new User();
        user.setFullName(requestDto.getFull_name());
        user.setPhone(requestDto.getPhone());
        user.setEmail(requestDto.getEmail());
        user.setRole(role);
        user.setAddress(requestDto.getAddress());
        // Hashing password
        String originalPassword = requestDto.getPassword();
        String hashedPassword = BCrypt.withDefaults().hashToString(12, originalPassword.toCharArray());
        user.setPassword(hashedPassword);

        BankInfo bankInfo = null;
        if (requestDto.getBank_info() != null) {
            bankInfo = new BankInfo();
            bankInfo.setBankName(requestDto.getBank_info().getBank_name());
            bankInfo.setAccountNumber(requestDto.getBank_info().getAccount_number());

        }

        Profile profile = new Profile();
        profile.setProfileImageBase64(requestDto.getProfileImageBase64());
        profile.setBank_info(bankInfo);


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
}


