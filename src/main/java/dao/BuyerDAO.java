package dao;

import dto.MenuDto;
import dto.MessageDto;
import entity.FoodItem;
import entity.Restaurant;
import entity.User;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import service.exception.MenuServiceExceptions;
import service.exception.RestaurantServiceExceptions;
import service.exception.UserNotFoundException;
import util.HibernateUtil;

import entity.Menu;

import java.util.*;


public class BuyerDAO {

    public List<Restaurant> SearchVendors(String searchTerm, List<String> keywords) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            StringBuilder hql = new StringBuilder("SELECT DISTINCT r FROM Restaurant r "); // Use SELECT DISTINCT r to avoid duplicate restaurants

            List<String> conditions = new ArrayList<>();
            Map<String, Object> params = new HashMap<>();
            // Condition 1: Search term in Restaurant name or address
            if (searchTerm != null && !searchTerm.isBlank()) {
                conditions.add("r.name LIKE :searchTerm OR r.address LIKE :searchTerm");
                params.put("searchTerm", "%" + searchTerm + "%");
            }
            // Condition 2: Search term in FoodItem name or description
            if (searchTerm != null && !searchTerm.isBlank()) {
                conditions.add("EXISTS (SELECT 1 FROM r.menus m_fi JOIN m_fi.foodItems fi_name_desc WHERE fi_name_desc.name LIKE :searchTerm OR fi_name_desc.description LIKE :searchTerm)");
            }
            // Condition 3: Keywords in FoodItem keywords
            if (keywords != null && !keywords.isEmpty()) {
                // Use EXISTS for efficient check with element collection
                conditions.add("EXISTS (SELECT 1 FROM r.menus m_k JOIN m_k.foodItems fi_k JOIN fi_k.keywords k WHERE k IN (:keywords))");
                params.put("keywords", keywords); // Use setParameterList when setting in query
            }
            // Combine conditions with OR
            if (!conditions.isEmpty()) {
                hql.append(" WHERE ");
                hql.append(String.join(" OR ", conditions));
            }

            Query<Restaurant> query = session.createQuery(hql.toString(), Restaurant.class);
            // Set parameters
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                if (entry.getKey().equals("keywords")) {
                    query.setParameterList(entry.getKey(), (Collection) entry.getValue());
                } else {
                    query.setParameter(entry.getKey(), entry.getValue());
                }
            }

            return query.getResultList();

        } catch (Exception e) {
            e.printStackTrace();
            // Consider throwing a custom exception instead of a generic RuntimeException
            throw new RuntimeException("Error searching restaurants: " + e.getMessage(), e);
        }
    }

    public Optional<Restaurant> findVendorWithMenuAndItems(Long id){
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();

            Optional<Restaurant> optVendor = Optional.ofNullable(session.get(Restaurant.class, id));
            if (optVendor.isPresent()) {
                Restaurant vendor = optVendor.get();
                Hibernate.initialize(vendor.getMenus());
                for(Menu menu: vendor.getMenus()){
                    Hibernate.initialize(menu.getFoodItems());
                    for (FoodItem item : menu.getFoodItems()) {
                        Hibernate.initialize(item.getKeywords());
                    }
                }
            }

            transaction.commit();
            return optVendor;
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public List<FoodItem> getItemList(String searchTerm, Integer price, List<String> keywords) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {

            StringBuilder hql = new StringBuilder("SELECT DISTINCT fi FROM FoodItem fi JOIN FETCH fi.restaurant LEFT JOIN FETCH fi.keywords ");


            List<String> mainConditions = new ArrayList<>();
            List<String> orConditions = new ArrayList<>();

            Map<String, Object> params = new HashMap<>();

            mainConditions.add("fi.menus IS NOT EMPTY");

            if (searchTerm != null && !searchTerm.isBlank()) {
                orConditions.add("(fi.name LIKE :searchTerm OR fi.description LIKE :searchTerm)");
                params.put("searchTerm", "%" + searchTerm + "%");
            }

            if (keywords != null && !keywords.isEmpty()) {
                hql.append("JOIN fi.keywords k_filter ");
                orConditions.add("k_filter IN (:keywords)");
                params.put("keywords", keywords);
            }

            if (!orConditions.isEmpty()) {
                mainConditions.add("(" + String.join(" OR ", orConditions) + ")");
            }

            if (price != null && price > 0) {
                mainConditions.add("fi.price <= :price");
                params.put("price", price);
            }

            if (!mainConditions.isEmpty()) {
                hql.append(" WHERE ");
                hql.append(String.join(" AND ", mainConditions));
            }

            Query<FoodItem> query = session.createQuery(hql.toString(), FoodItem.class);

            for (Map.Entry<String, Object> entry : params.entrySet()) {
                if (entry.getValue() instanceof Collection) {
                    query.setParameterList(entry.getKey(), (Collection<?>) entry.getValue());
                } else {
                    query.setParameter(entry.getKey(), entry.getValue());
                }
            }

            return query.getResultList();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error searching food items: " + e.getMessage(), e);
        }
    }

    public Optional<FoodItem> findItem(Long id) throws RestaurantServiceExceptions {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<FoodItem> query = session.createQuery("SELECT foodItem FROM FoodItem foodItem JOIN FETCH foodItem.restaurant LEFT JOIN FETCH foodItem.keywords WHERE foodItem.id = :id", FoodItem.class);
            query.setParameter("id", id);
            return query.uniqueResultOptional();
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public Optional<User> findUserWithFavoriteRestaurants(String phone) throws UserNotFoundException {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<User> query = session.createQuery("SELECT user FROM User user LEFT JOIN FETCH user.favoriteRestaurants WHERE user.phone = :phone", User.class);
            query.setParameter("phone", phone);
            return query.uniqueResultOptional();
        } catch (Exception e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

}
