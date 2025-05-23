package HTTPhandler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import entity.User;
import entity.Profile;
import org.hibernate.Session;
import org.hibernate.Transaction;
import util.HibernateUtil;
import util.JwtUtil;
import dto.*;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ProfileHTTPHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String token = exchange.getRequestHeaders().getFirst("Authorization");

        String phone = JwtUtil.validateToken(token);
        if (phone == null) {
            exchange.sendResponseHeaders(401, -1); // Unauthorized
            return;
        }

        if ("GET".equals(exchange.getRequestMethod())) {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/auth/profile")) {
                handleGetProfile(exchange, phone);
            } else {
                exchange.sendResponseHeaders(404, -1); // Not Found
            }
        } else if ("PUT".equals(exchange.getRequestMethod())) {
            String path = exchange.getRequestURI().getPath();
            if (path.matches("/auth/profile/\\d+")) {
                handleUpdateProfile(exchange, phone);
            } else {
                exchange.sendResponseHeaders(404, -1); // Not Found
            }
        } else {
            exchange.sendResponseHeaders(405, -1); // Method Not Allowed
        }
    }

    private void handleGetProfile(HttpExchange exchange, String phone) throws IOException {
        // Fetch user and profile
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            User user = getUserByPhone(session, phone);
            if (user == null || user.getProfile() == null) {
                exchange.sendResponseHeaders(404, -1); // Not Found
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
                    profile.getUser().getFullName(),
                    profile.getUser().getPhone(),
                    profile.getUser().getEmail(),
                    profile.getUser().getRole().toString(),
                    profile.getUser().getAddress(),
                    profile.getProfileImageBase64(),
                    bankName,
                    accountNumber
            );

            String response = new Gson().toJson(profileDTO);

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }

        } catch (Exception e) {
            e.printStackTrace();
            exchange.sendResponseHeaders(500, -1); // Internal Server Error
        }
    }

    private void handleUpdateProfile(HttpExchange exchange, String phone) throws IOException {
        InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
        ProfileDto updatedProfile = new Gson().fromJson(reader, ProfileDto.class);

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();

            User user = getUserByPhone(session, phone);
            if (user == null) {
                exchange.sendResponseHeaders(404, -1); // Not Found
                return;
            }
            if(updatedProfile.getPhone() != null) {
                if (IsPhoneTaken(session, updatedProfile.getPhone())) {
                    exchange.sendResponseHeaders(403, -1); // Forbidden
                }
            }

            if (updatedProfile.getFull_name() != null) user.setFullName(updatedProfile.getFull_name());
            if (updatedProfile.getPhone() != null) user.setPhone(updatedProfile.getPhone());
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

}
