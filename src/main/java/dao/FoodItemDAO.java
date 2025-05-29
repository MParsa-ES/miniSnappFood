package dao;

import entity.FoodItem;
import entity.Restaurant;
import org.hibernate.Session;
import org.hibernate.Transaction;
import util.HibernateUtil;

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

}
