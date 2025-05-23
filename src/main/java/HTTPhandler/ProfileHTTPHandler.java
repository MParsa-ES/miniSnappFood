package HTTPhandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import entity.User;
import entity.Profile;
import entity.InvalidToken;
import org.hibernate.Session;
import org.hibernate.Transaction;
import util.HibernateUtil;
import util.JwtUtil;
import dto.*;
import util.RateLimiter;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ProfileHTTPHandler implements HttpHandler {

    private final Gson gson = new GsonBuilder().serializeNulls().create();

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        // Checking correct Header for content type
//        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
//        if (contentType == null || !contentType.toLowerCase().contains("application/json")) {
//            sendResponse(exchange, 415, "{\n\"error\":\"Unsupported media type\"\n}");
//            return;
//        }

        String ip = exchange.getRemoteAddress().getAddress().getHostAddress();
        if (!RateLimiter.isAllowed(ip)){
            sendResponse(exchange, 429, "{\n\"error\":\"Too many requests\"\n}");
            return;
        }

        String token = exchange.getRequestHeaders().getFirst("Authorization");

        if (token == null || token.isEmpty()) {
            sendResponse(exchange,401,"{\n\"error\":\"Unauthorized request\"\n}"); // Unauthorized
            return;
        }

        // Check if the token is in BlockList
        if (isTokenBlocklisted(token)) {
            sendResponse(exchange,401,"{\n\"error\":\"Unauthorized request\"\n}"); // Unauthorized
            return;
        }

        String phone = JwtUtil.validateToken(token);
        if (phone == null) {
            sendResponse(exchange,401,"{\n\"error\":\"Unauthorized request\"\n}"); // Unauthorized
            return;
        }

        if ("GET".equals(exchange.getRequestMethod())) {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/auth/profile")) {
                handleGetProfile(exchange, phone);
            } else {
                sendResponse(exchange,404,"{\n\"error\":\"Resource not found\"\n}"); // Not Found
            }
        } else if ("PUT".equals(exchange.getRequestMethod())) {
            String path = exchange.getRequestURI().getPath();
            if (path.matches("/auth/profile")) {
                handleUpdateProfile(exchange, phone);
            } else {
                sendResponse(exchange,404,"{\n\"error\":\"Resource not found\"\n}"); // Not Found
            }
        } else {
            sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}"); // Method Not Allowed
        }
    }

    private void handleGetProfile(HttpExchange exchange, String phone) throws IOException {
        // Fetch user and profile
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            User user = getUserByPhone(session, phone);
            if (user == null || user.getProfile() == null) {
                sendResponse(exchange,404,"{\n\"error\":\"Resource not found\"\n}"); // Not Found
                return;
            }

            Profile profile = user.getProfile();

            String bankName = "";
            String accountNumber = "";
            if (profile.getBank_info() != null) {
                bankName = profile.getBank_info().getBank_name();
                accountNumber = profile.getBank_info().getAccount_number();
            }

            ProfileDto profileDTO = new ProfileDto(
                    user.getId(),
                    user.getFullName(),
                    user.getPhone(),
                    user.getEmail(),
                    user.getRole().toString(),
                    user.getAddress(),
                    profile.getProfileImageBase64(),
                    bankName,
                    accountNumber
            );

            String response = gson.toJson(profileDTO);

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            sendResponse(exchange, 200, response);

        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "{\"error\":\"Internal server error\"}"); // Internal Server Error
        }
    }

    private void handleUpdateProfile(HttpExchange exchange, String phone) throws IOException {
        InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
        ProfileDto updatedProfile = gson.fromJson(reader, ProfileDto.class);

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();

            User user = getUserByPhone(session, phone);
            if (user == null) {
                sendResponse(exchange,404,"{\n\"error\":\"Resource not found\"\n}"); // Not Found
                return;
            }
            if(updatedProfile.getPhone() != null) {
                if (IsPhoneTaken(session, updatedProfile.getPhone())) {
                    sendResponse(exchange, 403, "{\n\"error\":\"Forbidden request\"\n}"); // Forbidden
                    return;
                }
                // setting the new phone number
                user.setPhone(updatedProfile.getPhone());
            }

            if (updatedProfile.getFull_name() != null) user.setFullName(updatedProfile.getFull_name());
            if (updatedProfile.getEmail() != null) user.setEmail(updatedProfile.getEmail());
            if (updatedProfile.getAddress() != null) user.setAddress(updatedProfile.getAddress());

            Profile profile = user.getProfile();
            if (updatedProfile.getProfileImageBase64() != null) profile.setProfileImageBase64(updatedProfile.getProfileImageBase64());

            if (updatedProfile.getBank_info() != null && !user.getRole().toString().equals("BUYER")) {
                if (profile.getBank_info() == null) {
                    profile.setBank_info(new entity.BankInfo());
                    if (updatedProfile.getBank_info().getBank_name() != null) {
                        profile.getBank_info().setBank_name(updatedProfile.getBank_info().getBank_name());
                    }
                    if (updatedProfile.getBank_info().getAccount_number() != null) {
                        profile.getBank_info().setAccount_number(updatedProfile.getBank_info().getAccount_number());
                    }
                }
                else {
                    if (updatedProfile.getBank_info().getBank_name() != null) {
                        profile.getBank_info().setBank_name(updatedProfile.getBank_info().getBank_name());
                    }
                    if (updatedProfile.getBank_info().getAccount_number() != null) {
                        profile.getBank_info().setAccount_number(updatedProfile.getBank_info().getAccount_number());
                    }
                }
            }

            session.saveOrUpdate(user);
            tx.commit();

            String response = "Profile updated successfully";
            exchange.sendResponseHeaders(200, response.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
            exchange.sendResponseHeaders(500, -1); // Internal Server Error
        }
    }

    private User getUserByPhone(Session session, String phone) {
        return session.createQuery("from User where phone = :phone", User.class)
                .setParameter("phone", phone)
                .uniqueResult();
    }

    private Boolean IsPhoneTaken(Session session, String phone) {
        User user = getUserByPhone(session, phone);
        return user != null;
    }

    private boolean isTokenBlocklisted(String token) {
        String jti = JwtUtil.getJtiFromToken(token);
        // If token has no valid JTI it must be invalid
        if (jti == null) {
            return true;
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            InvalidToken foundToken = session.createQuery("from InvalidToken where jti = :jti", InvalidToken.class)
                    .setParameter("jti", jti)
                    .uniqueResult();
            // اگر رکوردی پیدا شد (foundToken != null)، یعنی توکن باطل شده است
            return foundToken != null;
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String responseBody) throws IOException {
        byte[] responseBodyBytes = responseBody.getBytes();
        exchange.sendResponseHeaders(statusCode, responseBodyBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBodyBytes);
        }
    }

}
