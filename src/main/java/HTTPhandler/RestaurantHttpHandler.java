package HTTPhandler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import dto.RestaurantDto;
import entity.Restaurant;
import entity.User;
import org.hibernate.Session;
import org.hibernate.Transaction;
import util.HibernateUtil;
import util.JwtUtil;
import util.RateLimiter;
import util.Utils;

import java.io.IOException;
import java.io.InputStreamReader;

public class RestaurantHttpHandler implements HttpHandler {

    private final Gson gson = new Gson();

    public void handle(HttpExchange t) throws IOException {

        if (!Utils.isTokenValid(t)) return;

        String ip = t.getRemoteAddress().getAddress().getHostAddress();
        if (!RateLimiter.isAllowed(ip)) {
            Utils.sendResponse(t, 429, "{\n\"error\":\"Too many requests\"\n}");
            return;
        }

        switch(t.getRequestURI().getPath()) {
            case "/restaurants":
                createRestaurant(t);
        }
    }

    private void createRestaurant(HttpExchange exchange) throws IOException {

        if (!exchange.getRequestMethod().equals("POST")) {
            Utils.sendResponse(exchange, 405, "{\"error\":\"Method not allowed\"}");
            return;
        }

        if (Utils.checkUnathorizedMediaType(exchange)) return;

        InputStreamReader reader = new InputStreamReader(exchange.getRequestBody());
        RestaurantDto.Request requestDto = gson.fromJson(reader, RestaurantDto.Request.class);

        if (requestDto.getName() == null || requestDto.getAddress() == null || requestDto.getPhone() == null) {
            Utils.sendResponse(exchange, 405, "{\n\"error\":\"Invalid field name\"\n}");
            return;
        }

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            String userPhone = JwtUtil.validateToken(exchange.getRequestHeaders().getFirst("Authorization"));
            User user = Utils.getUserByPhone(session, userPhone);

            // User role is not seller
            if (!(user.getRole().toString().equals("SELLER"))) {
                Utils.sendResponse(exchange, 403, "{\"error\":\"Forbidden request\"}");
            }

            // Phone already exists
            if (Utils.getRestaurantByPhone(session, requestDto.getPhone()) != null) {
                Utils.sendResponse(exchange, 409, "{\"error\":\"Conflict occurred\"}");
                return;
            }

            Restaurant restaurant = new Restaurant();
            restaurant.setName(requestDto.getName());
            restaurant.setAddress(requestDto.getAddress());
            restaurant.setPhone(requestDto.getPhone());
            restaurant.setOwner(user);
            Transaction transaction = session.beginTransaction();
            session.save(restaurant);
            transaction.commit();

            RestaurantDto.Response responseDto = new RestaurantDto.Response();
            responseDto.setId(restaurant.getId());
            responseDto.setName(requestDto.getName());
            responseDto.setAddress(requestDto.getAddress());
            responseDto.setPhone(requestDto.getPhone());
            responseDto.setTax_fee(requestDto.getTax_fee());
            responseDto.setAdditional_fee(requestDto.getAdditional_fee());
            Utils.sendResponse(exchange, 201, gson.toJson(responseDto));

        } catch (Exception e) {
            e.printStackTrace();
            Utils.sendResponse(exchange, 500, "{\"error\":\"Internal server error\"}");
        }
    }

}
