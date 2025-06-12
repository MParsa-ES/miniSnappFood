package dao;

import entity.Order;
import org.hibernate.Session;
import org.hibernate.Transaction;
import util.HibernateUtil;


public class OrderDAO {

    public void save(Order order) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.save(order);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Error in saving order:" + e.getMessage(), e);
        }
    }

}
