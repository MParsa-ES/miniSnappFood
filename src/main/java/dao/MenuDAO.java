package dao;

import entity.Menu;
import org.hibernate.Hibernate;
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

    public Optional<Menu> findMenuInRestaurant(String title, Long restaurantId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Menu> query = session.createQuery("from Menu m where m.restaurant.id=:restaurantId and m.title = :title", Menu.class);
            query.setParameter("restaurantId", restaurantId);
            query.setParameter("title", title);
            Optional<Menu> menu = query.uniqueResultOptional();
            if (menu.isPresent()) {
                Hibernate.initialize(menu.get().getFoodItems());
            }
            return menu;
        } catch (Exception e) {
            throw new RuntimeException("Error while checking for menu with title :" + title + e.getMessage(), e);
        }
    }

    public void deleteById(Long id) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            Menu menu = session.get(Menu.class, id);
            session.delete(menu);
            transaction.commit();
        } catch (Exception e){
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException("Error while deleting menu with id :" + id + e.getMessage(), e);
        }
    }

    public void update(Menu menu) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.merge(menu);
            transaction.commit();
        } catch (Exception e){
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException("Error while updating menu with id :" + menu.getId() + e.getMessage(), e);
        }
    }
}
