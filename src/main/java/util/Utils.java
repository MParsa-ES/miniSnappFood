package util;
import com.sun.net.httpserver.HttpExchange;
import entity.InvalidToken;
import entity.Restaurant;
import entity.User;
import org.hibernate.Session;
import java.io.IOException;
import java.io.OutputStream;

public class Utils {

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

    public static boolean isTokenValid(HttpExchange exchange) throws IOException {
        String token = exchange.getRequestHeaders().getFirst("Authorization");
        String jti = JwtUtil.getJtiFromToken(token);
        boolean tokenBlockListed = false;

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
}
