package dao;

import entity.Restaurant;
import org.hibernate.Session;
import org.hibernate.query.Query;
import util.HibernateUtil;

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
}
