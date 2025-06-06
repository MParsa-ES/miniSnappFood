package dao;

import entity.FoodItem;
import entity.Restaurant;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import util.HibernateUtil;

import java.util.Optional;

public class FoodItemDAO {

    public FoodItem save(FoodItem foodItem) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.save(foodItem);
            transaction.commit();
            return foodItem; // The foodItem object now has its ID if generated
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            System.err.println("Error saving foodItem: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Could not save food item");
        }
    }

    public Optional<FoodItem> findFoodItemById(Long RestaurantId, Long FoodItemId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<FoodItem> query = session.createQuery("FROM FoodItem WHERE id = :FoodItemId AND  restaurant.id = :RestaurantId", FoodItem.class);
            query.setParameter("FoodItemId", FoodItemId);
            query.setParameter("RestaurantId", RestaurantId);
            return query.uniqueResultOptional();
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public FoodItem update(FoodItem foodItem) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.merge(foodItem);
            transaction.commit();
            return foodItem;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException("Could not update foodItem");
        }
    }

    public void delete(FoodItem foodItem) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.delete(foodItem);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException("Could not delete foodItem");
        }
    }

}
