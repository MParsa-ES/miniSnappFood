package util;
import com.sun.net.httpserver.HttpExchange;
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
}
