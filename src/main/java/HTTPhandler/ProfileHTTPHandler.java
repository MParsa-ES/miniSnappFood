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
            if (profile.getBankInfo() != null) {
                bankName = profile.getBankInfo().getBankName();
                accountNumber = profile.getBankInfo().getAccountNumber();
            }

            ProfileDto profileDTO = new ProfileDto(
                    profile.getUser().getFullName(),
                    profile.getUser().getPhone(),
                    profile.getUser().getEmail(),
                    profile.getUser().getRole().toString(),
                    profile.getUser().getAddress(),
                    profile.getProfilePicture(),
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

            // بروزرسانی فیلدهای کاربر (User) انتخابی:
            if (updatedProfile.getFullName() != null) user.setFullName(updatedProfile.getFullName());
            if (updatedProfile.getPhone() != null) user.setPhone(updatedProfile.getPhone());
            if (updatedProfile.getEmail() != null) user.setEmail(updatedProfile.getEmail());
            if (updatedProfile.getAddress() != null) user.setAddress(updatedProfile.getAddress());

            Profile profile = user.getProfile();
            if (updatedProfile.getProfilePicture() != null) profile.setProfilePicture(updatedProfile.getProfilePicture());

            // بروزرسانی اطلاعات بانکی:
            if (updatedProfile.getBankInfo() != null && !user.getRole().toString().equals("BUYER")) {
                if (profile.getBankInfo() == null) {
                    // اگر موجود نباشد، یک موجودیت جدید بانک ایجاد کنید
                    profile.setBankInfo(new entity.BankInfo());
                    if (updatedProfile.getBankInfo().getBankName() != null) {
                        profile.getBankInfo().setBankName(updatedProfile.getBankInfo().getBankName());
                    }
                    if (updatedProfile.getBankInfo().getAccountNumber() != null) {
                        profile.getBankInfo().setAccountNumber(updatedProfile.getBankInfo().getAccountNumber());
                    }
                }
                else {
                    if (updatedProfile.getBankInfo().getBankName() != null) {
                        profile.getBankInfo().setBankName(updatedProfile.getBankInfo().getBankName());
                    }
                    if (updatedProfile.getBankInfo().getAccountNumber() != null) {
                        profile.getBankInfo().setAccountNumber(updatedProfile.getBankInfo().getAccountNumber());
                    }
                }
            }

            session.saveOrUpdate(user); // از cascade برای ذخیره پروفایل و بانک اطلاعاتی استفاده می‌شود
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
}
