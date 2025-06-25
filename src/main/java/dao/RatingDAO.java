package dao;

import entity.FoodItem;
import entity.Order;
import entity.Rating;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import util.HibernateUtil;

import java.util.List;
import java.util.Optional;

public class RatingDAO {

    public Rating save(Rating rating) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.save(rating);
            transaction.commit();
            return rating;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Error saving rating", e);
        }
    }


    public boolean doesRatingExist(Long userId, Long orderId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Long> query = session.createQuery(
                    "SELECT count(r.id) FROM Rating r WHERE r.user.id = :userId AND r.order.id = :orderId",
                    Long.class
            );
            query.setParameter("userId", userId);
            query.setParameter("orderId", orderId);

            Long count = query.uniqueResult();
            return count != null && count > 0;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Could not check for existing rating", e);
        }
    }

    public List<Rating> getItemRatings(Long itemId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Rating> query = session.createQuery(
                    "SELECT DISTINCT r FROM Rating r LEFT JOIN FETCH  r.user LEFT JOIN FETCH r.order o LEFT JOIN FETCH o.items oi WHERE oi.foodItem.id = :foodItemId", Rating.class);
            query.setParameter("foodItemId", itemId);
            return query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error searching ratings: " + e.getMessage(), e);
        }
    }

    public Optional<Rating> getRatingById(Long ratingId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Rating> query = session.createQuery("FROM Rating r LEFT JOIN FETCH r.user LEFT JOIN FETCH r.order o LEFT JOIN FETCH o.items oi " +
                    "LEFT JOIN FETCH oi.foodItem WHERE r.id = :ratingId", Rating.class);
            query.setParameter("ratingId", ratingId);
            return query.uniqueResultOptional();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error searching ratings: " + e.getMessage(), e);
        }
    }

    public void delete(Rating rating) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.delete(rating);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException("Could not delete rating");
        }
    }

    public void update(Rating rating) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.merge(rating);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Error in updating rating:" + e.getMessage(), e);
        }
    }

}