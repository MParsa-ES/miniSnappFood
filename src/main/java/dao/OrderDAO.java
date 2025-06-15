package dao;

import entity.Order;
import entity.OrderStatus;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import util.HibernateUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


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

    public Optional<Order> findOrderById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Order> query = session.createQuery(
                    "SELECT o FROM Order o LEFT JOIN FETCH o.restaurant LEFT JOIN FETCH o.items LEFT JOIN FETCH o.customer WHERE o.id = :orderId", Order.class);
            query.setParameter("orderId", id);
            return query.uniqueResultOptional();
        } catch (Exception e) {
            System.err.println("Error finding order by id " + id + ": " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public List<Order> findByRestaurantId(Long restaurantId, String status, String search, String user, String courier) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            Map<String, Object> params = new HashMap<>();

            StringBuilder hqlBuilder = new StringBuilder("SELECT DISTINCT o FROM Order o " +
                    "LEFT JOIN FETCH o.customer c " +
                    "LEFT JOIN FETCH o.items oi " +
                    "LEFT JOIN oi.foodItem fi " +
                    "WHERE o.restaurant.id = :restaurantId");
            params.put("restaurantId", restaurantId);

            if (status != null && !status.isBlank()) {
                hqlBuilder.append(" AND o.status = :status");
                params.put("status", OrderStatus.valueOf(status.toUpperCase()));
            }
            if (search != null && !search.isBlank()) {
                hqlBuilder.append(" AND fi.name LIKE :searchQuery");
                params.put("searchQuery", "%" + search + "%");
            }
            if (user != null && !user.isBlank()) {
                hqlBuilder.append(" AND (c.fullName LIKE :userName)");
                params.put("userName", "%" + user + "%");
            }

            // TODO: add after finishing the courier part
//            if (courier != null && !courier.isBlank()) {
//                hqlBuilder.append(" AND (o.courier.name LIKE :courierName)");
//            }

            hqlBuilder.append(" ORDER BY o.createdAt DESC");

            Query<Order> query = session.createQuery(hqlBuilder.toString(), Order.class);
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                query.setParameter(entry.getKey(), entry.getValue());
            }

            return query.list();
        } catch (Exception e) {
            System.err.println("Error finding order by restaurantId " + restaurantId + ": " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }

    public void updateOrder(Order order) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.merge(order);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Error in updating order:" + e.getMessage(), e);
        }
    }

    public List<Order> findOrdersAwaitingDelivery() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            Query<Order> query = session.createQuery(
                    "SELECT DISTINCT o FROM Order o " +
                            "LEFT JOIN FETCH o.items oi " +
                            "LEFT JOIN FETCH o.customer c " +
                            "LEFT JOIN FETCH o.restaurant r " +
                            "WHERE o.status = :status " +
                            "ORDER BY o.createdAt ASC", Order.class);

            query.setParameter("status", OrderStatus.FINDING_COURIER);
            return query.list();
        } catch (Exception e) {
            System.err.println("Error finding orders awaiting delivery: " + e.getMessage());
            return List.of();
        }
    }
}
