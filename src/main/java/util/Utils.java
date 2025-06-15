package util;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import dto.ErrorResponseDto;
import entity.InvalidToken;
import entity.User;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.io.IOException;
import java.io.OutputStream;

public class Utils {

    private final static Gson gson = new Gson();

    // Checking correct Header for content type
    public static boolean checkUnathorizedMediaType(HttpExchange exchange) throws IOException {

        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.toLowerCase().contains("application/json")) {
            sendResponse(exchange, 415, "{\n\"error\":\"Unsupported media type\"\n}");
            return true;
        }
        return false;
    }

    // sending a response to the api with the desired status code and messages
    public static void sendResponse(HttpExchange exchange, int statusCode, String responseBody) throws IOException {
        byte[] responseBodyBytes = responseBody.getBytes();
        exchange.sendResponseHeaders(statusCode, responseBodyBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBodyBytes);
        }
    }

    // checking the validity of the email
    public static boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return email.matches(emailRegex);
    }

    // get a user from his or her phone number
    public static User getUserByPhone(Session session, String phone) {
        return session.createQuery("from User where phone = :phone", User.class)
                .setParameter("phone", phone)
                .uniqueResult();
    }


    /**
     * @param exchange the http exchange
     * @return a boolean regarding the validity of the token in the exchnage
     * @deprecated use the new getAuthenticatedUserPhone function in this same package instead
     */
    public static boolean isTokenValid(HttpExchange exchange) {
        String token = exchange.getRequestHeaders().getFirst("Authorization");
        String jti = JwtUtil.getJtiFromToken(token);
        boolean tokenBlockListed;

        // If token has no valid JTI it must be invalid
        if (jti == null) {
            tokenBlockListed = true;
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            InvalidToken foundToken = session.createQuery("from InvalidToken where jti = :jti", InvalidToken.class)
                    .setParameter("jti", jti)
                    .uniqueResult();

            tokenBlockListed = foundToken != null;
        } catch (Exception e) {
            e.printStackTrace();
            tokenBlockListed = true;
        }

        String phone = JwtUtil.validateToken(token);

        if (token == null || token.isEmpty() || tokenBlockListed || phone == null) {
//            Utils.sendResponse(exchange, 401, "{\n\"error\":\"Unauthorized request\"\n}"); // Unauthorized
            return false;
        }
        return true;
    }

    public static String getAuthenticatedUserPhone(HttpExchange exchange) throws IOException {

        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");

        if (authHeader == null) {
            sendResponse(exchange, 401, gson.toJson(new ErrorResponseDto("Missing Authorization header.")));
            return null;
        }

        String token = authHeader.replace("Bearer ", "");

        if (token.isEmpty()) {
            sendResponse(exchange, 401, gson.toJson(new ErrorResponseDto("Missing token.")));
            return null;
        }

        String jti = JwtUtil.getJtiFromToken(token);
        String phone = JwtUtil.validateToken(token);

        if (jti == null || phone == null) {
            sendResponse(exchange, 401, gson.toJson(new ErrorResponseDto("Invalid token.")));
            return null;
        }

        boolean isBlacklisted = false;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<InvalidToken> query = session.createQuery("from InvalidToken where jti = :jti_value", InvalidToken.class);
            query.setParameter("jti_value", jti);
            if (query.uniqueResultOptional().isPresent()) {
                isBlacklisted = true;
            }
        } catch (Exception e) {
            System.err.println("Error checking token blacklist: " + e.getMessage());
            e.printStackTrace();
            Utils.sendResponse(exchange, 500, gson.toJson(new ErrorResponseDto("Internal server error during authentication.")));
            return null;
        }

        if (isBlacklisted){
            sendResponse(exchange, 401, gson.toJson(new ErrorResponseDto("Token is blacklisted.")));
            return null;
        }

        return phone;
    }
}
