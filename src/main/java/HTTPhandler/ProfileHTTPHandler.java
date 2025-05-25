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
import dto.ProfileDto;
import util.RateLimiter;
import util.Utils;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ProfileHTTPHandler implements HttpHandler {

    private final Gson gson = new GsonBuilder().serializeNulls().create();

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        String ip = exchange.getRemoteAddress().getAddress().getHostAddress();
        if (!RateLimiter.isAllowed(ip)) {
            Utils.sendResponse(exchange, 429, "{\n\"error\":\"Too many requests\"\n}");
            return;
        }

        String token = exchange.getRequestHeaders().getFirst("Authorization");

        if (token == null || token.isEmpty()) {
            Utils.sendResponse(exchange, 401, "{\n\"error\":\"Unauthorized request\"\n}"); // Unauthorized
            return;
        }

        // Check if the token is in BlockList
        if (isTokenBlocklisted(token)) {
            Utils.sendResponse(exchange, 401, "{\n\"error\":\"Unauthorized request\"\n}"); // Unauthorized
            return;
        }

        String phone = JwtUtil.validateToken(token);
        if (phone == null) {
            Utils.sendResponse(exchange, 401, "{\n\"error\":\"Unauthorized request\"\n}"); // Unauthorized
            return;
        }

        if ("GET".equals(exchange.getRequestMethod())) {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/auth/profile")) {
                handleGetProfile(exchange, phone);
            } else {
                Utils.sendResponse(exchange, 404, "{\n\"error\":\"Resource not found\"\n}"); // Not Found
            }
        } else if ("PUT".equals(exchange.getRequestMethod())) {
            String path = exchange.getRequestURI().getPath();
            if (path.matches("/auth/profile")) {
                handleUpdateProfile(exchange, phone);
            } else {
                Utils.sendResponse(exchange, 404, "{\n\"error\":\"Resource not found\"\n}"); // Not Found
            }
        } else {
            Utils.sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}"); // Method Not Allowed
        }
    }

    private void handleGetProfile(HttpExchange exchange, String phone) throws IOException {
        // Fetch user and profile
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            User user = Utils.getUserByPhone(session, phone);
            if (user == null || user.getProfile() == null) {
                Utils.sendResponse(exchange, 404, "{\n\"error\":\"Resource not found\"\n}"); // Not Found
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
            Utils.sendResponse(exchange, 200, response);

        } catch (Exception e) {
            e.printStackTrace();
            Utils.sendResponse(exchange, 500, "{\"error\":\"Internal server error\"}"); // Internal Server Error
        }
    }

    private void handleUpdateProfile(HttpExchange exchange, String phone) throws IOException {

        // Checking correct Header for content type
        if (Utils.checkUnathorizedMediaType(exchange)) return;

        InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
        ProfileDto updatedProfile = gson.fromJson(reader, ProfileDto.class);

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();

            User user = Utils.getUserByPhone(session, phone);
            if (user == null) {
                Utils.sendResponse(exchange, 404, "{\n\"error\":\"Resource not found\"\n}"); // Not Found
                return;
            }
            if (updatedProfile.getPhone() != null) {
                if (Utils.getUserByPhone(session, updatedProfile.getPhone()) == null) {
                    Utils.sendResponse(exchange, 403, "{\n\"error\":\"Forbidden request\"\n}"); // Forbidden
                    return;
                }
                // setting the new phone number
                user.setPhone(updatedProfile.getPhone());
            }

            if (updatedProfile.getFull_name() != null) user.setFullName(updatedProfile.getFull_name());
            if (updatedProfile.getEmail() != null) {
                if (Utils.isValidEmail(updatedProfile.getEmail())) {
                    user.setEmail(updatedProfile.getEmail());
                } else {
                    Utils.sendResponse(exchange, 403, "{\n\"error\":\"Invalid field name\"\n}");
                }
            }

            if (updatedProfile.getAddress() != null) user.setAddress(updatedProfile.getAddress());

            Profile profile = user.getProfile();
            if (updatedProfile.getProfileImageBase64() != null)
                profile.setProfileImageBase64(updatedProfile.getProfileImageBase64());

            // TODO this can be done in another way to return 403 forbidden request code
            if (updatedProfile.getBank_info() != null && !user.getRole().toString().equals("BUYER")) {
                if (profile.getBank_info() == null) {
                    profile.setBank_info(new entity.BankInfo());
                    if (updatedProfile.getBank_info().getBank_name() != null) {
                        profile.getBank_info().setBank_name(updatedProfile.getBank_info().getBank_name());
                    }
                    if (updatedProfile.getBank_info().getAccount_number() != null) {
                        profile.getBank_info().setAccount_number(updatedProfile.getBank_info().getAccount_number());
                    }
                } else {
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

            Utils.sendResponse(exchange, 200, "{\n\"message\":\"Profile updated successfully\"\n}");
        } catch (Exception e) {
            e.printStackTrace();
            Utils.sendResponse(exchange, 500, "{\"error\":\"Internal server error\"}"); // Internal Server Error
        }
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
}
