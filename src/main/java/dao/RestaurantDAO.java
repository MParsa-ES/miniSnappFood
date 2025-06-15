package dao;

import entity.Restaurant;
import entity.User;

import util.HibernateUtil;
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
            return restaurant;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            e.printStackTrace();
            throw new RuntimeException("Could not save restaurant");
        }
    }

    public Optional<Restaurant> findRestaurantByOwner(User owner) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Restaurant> query = session.createQuery("FROM Restaurant WHERE owner = :owner", Restaurant.class);
            query.setParameter("owner", owner);
            return query.uniqueResultOptional();
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public Optional<Restaurant> findRestaurantById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Restaurant> query = session.createQuery("FROM Restaurant WHERE id = :id", Restaurant.class);
            query.setParameter("id", id);
            return query.uniqueResultOptional();
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public Restaurant update(Restaurant restaurant) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.merge(restaurant);
            transaction.commit();
            return restaurant;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException("Could not update restaurant");
        }
    }
}