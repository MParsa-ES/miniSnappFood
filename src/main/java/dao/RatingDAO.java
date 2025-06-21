package dao;

import entity.FoodItem;
import entity.Rating;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import util.HibernateUtil;

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


}