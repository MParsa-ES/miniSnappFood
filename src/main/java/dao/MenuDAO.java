package dao;

import entity.Menu;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import util.HibernateUtil;

import java.util.Optional;

public class MenuDAO {

    public Menu save(Menu menu) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.save(menu);
            transaction.commit();
            return menu;
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException("Error saving menu:" + e.getMessage(), e);
        }
    }

    public Optional<Menu> duplicateMenu(String title, Long restaurantId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Menu> query = session.createQuery("from Menu m where m.restaurant.id=:restaurantId and m.title =: title", Menu.class);
            query.setParameter("restaurantId", restaurantId);
            query.setParameter("title", title);
            return query.uniqueResultOptional();
        } catch (Exception e) {
            throw new RuntimeException("Error while checking duplicate menu:" + e.getMessage(), e);
        }
    }
}
