package dao;

import entity.Restaurant; // Assuming your entities are in this package
import util.HibernateUtil; // Assuming this is your HibernateUtil location
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import java.util.Optional;

public class RestaurantDAO {
    public Optional<Restaurant> findByPhone(String phone) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Restaurant> query = session.createQuery("FROM Restaurant WHERE phone = :phoneNumber", Restaurant.class);
            query.setParameter("phoneNumber", phone);
            return query.uniqueResultOptional();
        } catch (Exception e) {
            System.err.println("Error finding restaurant by phone: " + phone + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public Restaurant save(Restaurant restaurant) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.save(restaurant); // save() is fine, persist() is JPA standard. Both work.
            // save() returns the generated identifier, and modifies the passed object.
            transaction.commit();
            return restaurant; // The restaurant object now has its ID if generated
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            System.err.println("Error saving restaurant: " + e.getMessage());
            e.printStackTrace();
            // Consider rethrowing a more specific persistence exception
            throw new RuntimeException("Could not save restaurant: " + e.getMessage(), e);
        }
    }
}