package dao;

import entity.*;
import org.hibernate.Session;
import org.hibernate.query.Query;
import util.HibernateUtil;

import java.util.*;

public class TransactionDAO {

    public boolean hasSuccessfulTransaction(Long orderId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Long> query = session.createQuery(
                    "SELECT count(t.id) FROM Transaction t WHERE t.order.id = :orderId AND t.status = :status",
                    Long.class
            );

            query.setParameter("orderId", orderId);
            query.setParameter("status", TransactionStatus.SUCCESS);

            Long count = query.uniqueResult();
            return count != null && count > 0;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Could not check for existing successful transaction", e);
        }
    }

    public Transaction save(Transaction Transaction) {
        org.hibernate.Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.save(Transaction);
            transaction.commit();
            return Transaction;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Error saving transaction", e);
        }
    }

    public List<Transaction> getUserTransactions(Long userId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Transaction> query = session.createQuery(
                    "SELECT DISTINCT t FROM Transaction t LEFT JOIN FETCH t.order WHERE t.user.id = :userId ORDER BY t.createdAt DESC", Transaction.class);
            query.setParameter("userId", userId);
            return query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error searching ratings: " + e.getMessage(), e);
        }
    }

    public List<Transaction> searchTransactions(String search, String user, String method, String status) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            StringBuilder hql = new StringBuilder(
                    "SELECT DISTINCT t FROM Transaction t " +
                            "LEFT JOIN FETCH t.user u " +
                            "LEFT JOIN FETCH t.order o " +
                            "LEFT JOIN FETCH o.restaurant r "
            );

            List<String> conditions = new ArrayList<>();
            Map<String, Object> params = new HashMap<>();

            if (search != null && !search.isBlank()) {
                conditions.add("r.name LIKE :searchTerm");
                params.put("searchTerm", "%" + search + "%");
            }

            if (user != null && !user.isBlank()) {
                conditions.add("u.fullName LIKE :userName");
                params.put("userName", "%" + user + "%");
            }

            if (method != null && !method.isBlank()) {
                try {
                    conditions.add("t.method = :method");
                    params.put("method", TransactionMethod.valueOf(method.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    System.err.println("Invalid transaction method: " + method);
                }
            }

            if (status != null && !status.isBlank()) {
                try {
                    conditions.add("t.status = :status");
                    params.put("status", TransactionStatus.valueOf(status.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    System.err.println("Invalid transaction status: " + status);
                }
            }

            if (!conditions.isEmpty()) {
                hql.append(" WHERE ").append(String.join(" AND ", conditions));
            }

            hql.append(" ORDER BY t.createdAt DESC");

            Query<Transaction> query = session.createQuery(hql.toString(), Transaction.class);

            for (Map.Entry<String, Object> entry : params.entrySet()) {
                query.setParameter(entry.getKey(), entry.getValue());
            }

            return query.getResultList();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error searching transactions for admin", e);
        }
    }

}
