package entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "FoodItems")
@Getter
@Setter
@EqualsAndHashCode(of = "id")
public class FoodItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column
    private int price;

    @Column
    private int supply;

    @Lob
    private String imageBase64;

    @ElementCollection
    private Set<String> keywords;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @ManyToMany
    @JoinTable(name = "foodItem_category",
            joinColumns = @JoinColumn(name = "food_item_id"), /* Foreign Key to this Class (FoodItem) */
            inverseJoinColumns = @JoinColumn(name = "category_id") /* Foreign Key to Other Class (Category) */ )
    private Set<Category> categories;

    @ManyToMany(mappedBy = "foodItems")
    private Set<Menu> menus = new HashSet<>();

}