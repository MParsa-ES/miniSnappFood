package HTTPhandler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import entity.User;
import org.hibernate.Session;
import org.hibernate.Transaction;
import util.HibernateUtil;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class UserHTTPHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (exchange.getRequestMethod().equals("POST")) {
            String urlPath = exchange.getRequestURI().getPath();
            if (urlPath.equals("/auth/register")) {
                hadnleLogin(exchange);
            } else if (urlPath.equals("/auth/login")) {
                hadnleLogin(exchange);
            } else {
                exchange.sendResponseHeaders(400, -1); // Not Found
            }
        } else {
            exchange.sendResponseHeaders(405, -1); // Method not allowed
        }
    }

    private void hadnleSignUp(HttpExchange exchange) throws IOException {
        InputStreamReader data = new InputStreamReader(exchange.getRequestBody());

        User user = new Gson().fromJson(data, User.class);

        try(Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();

            session.save(user);
            // add Profile and save it
            transaction.commit();

            Map<String,String> response = new HashMap<>();
            response.put("message", "User registered successfully");
            response.put("userId", "" + user.getId());
            response.put("token","example token"); // should create token at later stage
            String json = new Gson().toJson(response);
            exchange.sendResponseHeaders(200, json.getBytes().length);
             try (OutputStream os = exchange.getResponseBody()) {
                 os.write(json.getBytes());
             } catch (Exception e) {
                 e.printStackTrace();
                 String message = "Failed to save user";
                 exchange.sendResponseHeaders(500, message.getBytes().length); // internal server error

             }
        }
    }

    private void hadnleLogin(HttpExchange exchange) throws IOException {
        InputStreamReader data = new InputStreamReader(exchange.getRequestBody());

    }

//    private boolean isPhoneNumberTaken(Session session, String phone) {
//        User user = session.createQuery("from user where phone = :phone", User.class)
//                .setParameter("phone", phone)
//                .uniqueResult();
//        return user != null;
//    }
}
