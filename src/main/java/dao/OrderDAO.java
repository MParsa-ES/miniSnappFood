package dao;

import entity.Order;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import util.HibernateUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


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

    public List<Order> findHistoryByCustomer(Long customerId, String vendor, String search) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            StringBuilder hqlBuilder = new StringBuilder("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items oi" +
                    " LEFT JOIN oi.foodItem fi WHERE o.customer.id = :customerId");
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("customerId", customerId);


            if (vendor != null && !vendor.isBlank()) {
                hqlBuilder.append(" AND o.restaurant.name LIKE :vendorName");
                parameters.put("vendorName", "%" + vendor + "%");
            }

            if (search != null && !search.isBlank()) {

                hqlBuilder.append(" AND (o.restaurant.name LIKE :searchQuery OR fi.name LIKE :searchQuery)");
                parameters.put("searchQuery", "%" + search + "%");
            }


            hqlBuilder.append(" ORDER BY o.createdAt DESC");

            Query<Order> query = session.createQuery(hqlBuilder.toString(), Order.class);

            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                query.setParameter(entry.getKey(), entry.getValue());
            }


            return query.list();

        } catch (Exception e) {
            System.err.println("Error finding order history for customer ID " + customerId + ": " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }

}
