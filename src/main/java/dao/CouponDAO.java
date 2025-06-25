package dao;

import entity.Coupon;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import util.HibernateUtil;

import java.util.List;
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
            Coupon coupon = session.get(Coupon.class, couponId);
            if (coupon != null) {
                return Optional.of(coupon);
            }
            return Optional.empty();
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
            session.delete(session.merge(coupon));
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            System.err.println("Error while deleting coupon: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error while deleting coupon: " + e.getMessage(), e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    public List<Coupon> findAllCoupons() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<Coupon> query = session.createQuery("FROM Coupon", Coupon.class);
            return query.list();
        }catch (Exception e) {
            System.err.println("Error while finding all coupons: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }

    }
}
