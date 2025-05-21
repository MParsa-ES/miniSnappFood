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

            String responese = "User registered successfully";
            exchange.sendResponseHeaders(200, responese.length());
             try (OutputStream os = exchange.getResponseBody()) {
                 os.write(responese.getBytes());
             } catch (IOException e) {
                 e.printStackTrace();

             }

        }
    }

    private void hadnleLogin(HttpExchange exchange) throws IOException {
        InputStreamReader data = new InputStreamReader(exchange.getRequestBody());

    }

//    private boolean isPhoneNumberTaken(Session session, String phone) {
//        User member = session.createQuery("from User where phone = :phone", User.class)
//                .setParameter("phone", phone)
//                .uniqueResult();
//        return member != null;
//    }
}
