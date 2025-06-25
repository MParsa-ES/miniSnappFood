package dao;

import entity.Coupon;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import util.HibernateUtil;

import java.util.Optional;

public class CouponDAO {

    public void save(Coupon coupon) {

        Session session = null;
        Transaction transaction = null;

        try {
            session = HibernateUtil.getSessionFactory().openSession();
            transaction = session.beginTransaction();
            session.save(coupon);
            transaction.commit();
        } catch (Exception e) {
            System.err.println("Error while saving coupon: " + e.getMessage());
            e.printStackTrace();
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Error while saving coupon: " + e.getMessage());
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    public Optional<Coupon> findCouponByCode(String couponCode) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Coupon> query = session.createQuery("FROM Coupon c WHERE c.couponCode = :couponCode", Coupon.class);
            query.setParameter("couponCode", couponCode);
            return query.uniqueResultOptional();
        } catch (Exception e) {
            System.err.println("Error while checking for coupon with code: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }


    public Optional<Coupon> findCouponById(Long couponId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return Optional.of(session.get(Coupon.class, couponId));
        } catch (Exception e) {
            System.err.println("Error while finding Coupon with ID" + couponId + ": " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public void delete(Coupon coupon) {
        Session session = null;
        Transaction transaction = null;

        try {
            session = HibernateUtil.getSessionFactory().openSession();
            transaction = session.beginTransaction();
            session.delete(coupon);
            transaction.commit();
        } catch (Exception e) {
            System.err.println("Error while deleting coupon: " + e.getMessage());
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Error while deleting coupon: " + e.getMessage());
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }
}
