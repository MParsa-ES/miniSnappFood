package dao;

import entity.Restaurant;
import entity.User;
import org.hibernate.Transaction;
import util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.List;
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

    public List<User> getAllUsers(){

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<User> query = session.createQuery("FROM User user LEFT JOIN FETCH user.profile p LEFT JOIN FETCH p.bank_info b", User.class);
            return query.list();
        } catch (Exception e) {
            System.err.println("Error getting all users: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }

    public Optional<User> findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<User> query = session.createQuery("FROM User u LEFT JOIN FETCH u.profile p LEFT JOIN FETCH p.bank_info WHERE u.id = :id", User.class);
            query.setParameter("id", id);
            return query.uniqueResultOptional();
        } catch (Exception e) {
            System.err.println("Error getting user with ID" + id + ": " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public void update(User user) {
        Transaction transaction = null;
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            transaction = session.beginTransaction();
            session.merge(user);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            System.err.println("Error updating user with ID" + user.getId() + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Could not update user with ID" + user.getId() + ": " + e.getMessage());
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    public void deleteById(Long userId) {
        Transaction transaction = null;
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            transaction = session.beginTransaction();
            session.delete(session.get(User.class, userId));
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            e.printStackTrace();
            System.err.println("Error deleting user with ID" + userId + ": " + e.getMessage());
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    public Long getTotalUsersCount() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Long> query = session.createQuery("SELECT COUNT(u.id) FROM User u", Long.class);
            return query.getSingleResult();
        }
    }
}