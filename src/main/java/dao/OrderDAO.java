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
        Session session = null;
        try{
            session = HibernateUtil.getSessionFactory().openSession();
            transaction = session.beginTransaction();
            session.save(order);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Error in saving order:" + e.getMessage(), e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    public void update(Order order) {
        Transaction transaction = null;
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            transaction = session.beginTransaction();
            session.merge(order);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            System.err.println("Error updating user with ID" + order.getId() + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Could not update user with ID" + order.getId() + ": " + e.getMessage());
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    public void save(Session session, Order order) {
        try {
            session.save(order);
        } catch (Exception e) {
            System.out.println("Error in saving order:" + e.getMessage());
            e.printStackTrace();
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
                    "SELECT o FROM Order o " +
                            "LEFT JOIN FETCH o.restaurant " +
                            "LEFT JOIN FETCH o.items " +
                            "LEFT JOIN FETCH o.customer " +
                            "LEFT JOIN FETCH o.courier " +
                            "WHERE o.id = :orderId", Order.class);
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

            // TODO: add after finishing the courier part -> think its done
            if (courier != null && !courier.isBlank()) {
                hqlBuilder.append(" AND (o.courier.name LIKE :courierName)");
            }

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

    public Optional<Order> findActiveOrderByCourierId(Long courierId){
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Order> query = session.createQuery("SELECT o FROM Order o WHERE o.courier.id = :courierId AND " +
                    "o.status IN (:statuses)", Order.class);
            query.setParameter("courierId", courierId);

            List<OrderStatus> statuses = List.of(
                    OrderStatus.ACCEPTED,
                    OrderStatus.ON_THE_WAY,
                    OrderStatus.RECEIVED);

            query.setParameterList("statuses", statuses);
            return query.uniqueResultOptional();
        } catch (Exception e){
            System.err.println("Error finding active order for courier with ID" + courierId + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error finding active order for courier: " + e.getMessage(), e);
        }

    }

    public List<Order> findOrdersHistoryByCourierId(Long courierId, String search, String vendor, String user){
        try(Session session = HibernateUtil.getSessionFactory().openSession()) {

            StringBuilder hql = new StringBuilder("SELECT DISTINCT o FROM Order o " +
                    "LEFT JOIN FETCH o.items oi " +
                    "LEFT JOIN oi.foodItem fi " +
                    "LEFT JOIN FETCH o.restaurant r " +
                    "LEFT JOIN FETCH o.customer cu " +
                    "LEFT JOIN FETCH o.courier co ");

            Map<String, Object> params = new HashMap<>();
            params.put("courierId", courierId);
            hql.append(" WHERE co.id = :courierId ");


            if (search != null && !search.isBlank()) {
                hql.append(" AND fi.name LIKE :searchQuery");
                params.put("searchQuery","%" + search + "%");
            }

            if (user != null && !user.isBlank()) {
                hql.append(" AND cu.fullName LIKE :userName ");
                params.put("userName","%" + user + "%");
            }

            if (vendor != null && !vendor.isBlank()) {
                hql.append(" AND r.name LIKE :vendor ");
                params.put("vendor","%" + vendor + "%");
            }

            hql.append(" ORDER BY o.createdAt DESC");

            Query<Order> query = session.createQuery(hql.toString(), Order.class);

            for (Map.Entry<String, Object> entry : params.entrySet()) {
                query.setParameter(entry.getKey(), entry.getValue());
            }

            return query.list();

        } catch (Exception e) {
            System.err.println("Error finding orders history by courierId " + courierId + ": " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }

    public List<Order> getAllOrdersWithFilters(String search, String vendor, String courier, String customer, String status) {

        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            HashMap<String, Object> params = new HashMap<>();


            StringBuilder hql = new StringBuilder("SELECT DISTINCT o FROM Order o " +
                    "LEFT JOIN FETCH o.restaurant r " +
                    "LEFT JOIN FETCH o.items oi " +
                    "LEFT JOIN FETCH oi.foodItem fi " +
                    "LEFT JOIN FETCH o.customer cu " +
                    "LEFT JOIN FETCH o.courier co " +
                    "WHERE 1=1");

            if (vendor != null && !vendor.isBlank()) {
                hql.append(" AND r.name LIKE :vendorName");
                params.put("vendorName","%" + vendor + "%");
            }

            if (courier != null && !courier.isBlank()) {
                hql.append(" AND co.fullName LIKE :courierName");
                params.put("courierName","%" + courier + "%");
            }

            if (customer != null && !customer.isBlank()) {
                hql.append(" AND cu.fullName LIKE :customerName");
                params.put("customerName","%" + customer + "%");
            }

            if (status != null && !status.isBlank()) {
                hql.append(" AND o.status = :status ");
                try {
                    params.put("status", OrderStatus.valueOf(status.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    System.err.println("Invalid status filter value: " + status);
                    return List.of();
                }
            }

            if (search != null && !search.isBlank()) {
                hql.append(" AND fi.name LIKE :searchQuery");
                params.put("searchQuery","%" + search + "%");
            }

            hql.append(" ORDER BY o.createdAt DESC");

            Query<Order> query = session.createQuery(hql.toString(), Order.class);

            for (Map.Entry<String, Object> entry : params.entrySet()) {
                query.setParameter(entry.getKey(), entry.getValue());
            }

            return query.list();
        } catch (Exception e) {
            System.err.println("Error finding all orders with filter: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }

}
