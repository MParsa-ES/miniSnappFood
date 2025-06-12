package dao;

import entity.Restaurant;
import entity.User;
import org.hibernate.Transaction;
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
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public Restaurant save(Restaurant restaurant) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.save(restaurant);
            transaction.commit();
            return restaurant; // The restaurant object now has its ID if generated
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            System.err.println("Error saving restaurant: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Could not save restaurant");
        }
    }

    public Optional<Restaurant> findRestaurant(User owner) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Restaurant> query = session.createQuery("FROM Restaurant WHERE owner = :owner", Restaurant.class);
            query.setParameter("owner", owner);
            return query.uniqueResultOptional();
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public User update(User user) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.merge(user); // یا session.update(user)
            transaction.commit();
            return user;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException("Could not update user", e);
        }
    }

}