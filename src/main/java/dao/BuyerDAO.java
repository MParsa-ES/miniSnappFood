package dao;

import entity.Restaurant;
import org.hibernate.Session;
import org.hibernate.query.Query;
import util.HibernateUtil;

import java.util.*;


public class BuyerDAO {
    public List<Restaurant> SearchVendors(String searchTerm, List<String> keywords) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            StringBuilder hql = new StringBuilder("FROM Restaurant r JOIN FETCH r.menus m JOIN FETCH m.foodItems fi WHERE 1=1");
            Map<String, Object> params = new HashMap<>();

            if (searchTerm != null && !searchTerm.isBlank()) {
                hql.append(" AND (r.name LIKE :searchTerm OR r.address LIKE :searchTerm)");
                params.put("searchTerm", "%" + searchTerm + "%");
            }

            if (keywords != null && !keywords.isEmpty()) {
                // This part requires careful handling for ElementCollection (keywords in FoodItem)
                // A common way is to use EXISTS or JOIN on the element collection table
                hql.append(" AND EXISTS (SELECT 1 FROM r.menus m_inner JOIN m_inner.foodItems fi_inner JOIN fi_inner.keywords k WHERE k IN :keywords)");
                params.put("keywords", keywords);
            }

            Query<Restaurant> query = session.createQuery(hql.toString(), Restaurant.class);
            params.forEach(query::setParameter);

            return query.getResultList();
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}
