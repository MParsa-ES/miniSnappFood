package dao;

import entity.User;
import util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;
import java.util.Optional;

public class UserDAO {
    public Optional<User> findByPhone(String phone) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<User> query = session.createQuery("FROM User WHERE phone = :phoneNumber", User.class);
            query.setParameter("phoneNumber", phone);
            return query.uniqueResultOptional();
        } catch (Exception e) {
            System.err.println("Error finding user by phone: " + phone + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }
}