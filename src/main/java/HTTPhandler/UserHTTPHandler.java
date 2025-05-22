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
                handleSignUp(exchange);
            } else if (urlPath.equals("/auth/login")) {
                handleLogin(exchange);
            } else {
                exchange.sendResponseHeaders(400, -1); // Not Found
            }
        } else {
            exchange.sendResponseHeaders(405, -1); // Method not allowed
        }
    }

    private void handleSignUp(HttpExchange exchange) throws IOException {
        InputStreamReader data = new InputStreamReader(exchange.getRequestBody());

        User user = new Gson().fromJson(data, User.class);

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            if (isPhoneNumberTaken(session, user.getPhone())){
                String response = "Phone number already exists";
                exchange.sendResponseHeaders(409, response.getBytes().length);
                try(OutputStream os = exchange.getResponseBody()){
                    os.write(response.getBytes());
                }
                return;
            }

            Transaction transaction = session.beginTransaction();

            try {
                session.save(user);
            } catch (Exception e) {
                String response = "Invalid input data";
                exchange.sendResponseHeaders(400, response.getBytes().length);
                try(OutputStream os = exchange.getResponseBody()){
                    os.write(response.getBytes());
                }
            }
            // add Profile and save it
            transaction.commit();

            Map<String, String> response = new HashMap<>();
            response.put("message", "User registered successfully");
            response.put("userId", "" + user.getId());
            response.put("token", "example token"); // should create token at later stage
            String json = new Gson().toJson(response);
            exchange.sendResponseHeaders(200, json.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(json.getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace();
            String response = "Failed to save user";
            exchange.sendResponseHeaders(500, response.getBytes().length); // internal server error
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        InputStreamReader data = new InputStreamReader(exchange.getRequestBody());
        User loginUser = new Gson().fromJson(data, User.class);


        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            User currentUser = getUserByPhone(session, loginUser.getPhone());

            if (currentUser == null || !loginUser.getPassword().equals(currentUser.getPassword())) {
                String response = "Invalid phone or password";
                exchange.sendResponseHeaders(400, response.getBytes().length);
                try(OutputStream os = exchange.getResponseBody()){
                    os.write(response.getBytes());
                }
                return;
            }
            

            // should generate token
            Map<String, String> response = new HashMap<>();
            response.put("message", "User logged in successfully");
            response.put("token", "example token");
            response.put("user", "sa");
            String json = new Gson().toJson(response);
            exchange.sendResponseHeaders(200, json.getBytes().length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(json.getBytes());
            }

        }

    }

    private boolean isPhoneNumberTaken(Session session, String phone) {
        User user = session.createQuery("from User where phone = :phone", User.class)
                .setParameter("phone", phone)
                .uniqueResult();
        return user != null;
    }

    private User getUserByPhone(Session session, String phone) {
        return session.createQuery("from User where phone = :phone",User.class)
                .setParameter("phone",phone)
                .uniqueResult();
    }
}
