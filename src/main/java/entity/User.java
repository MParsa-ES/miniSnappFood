package entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import entity.Role;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;


@Entity
@Getter
@Setter
@Table(name = "User")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(unique = true, nullable = false)
    private String phone;

    @Column()
    private String email;  // optional

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING) // Store enum as a readable string
    @Column(nullable = false)
    private Role role;


    @Column(nullable = false)
    private String address;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "profile_id" , nullable = false)
    private Profile profile;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_favorite_restaurants",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "restaurant_id")
    )
    private Set<Restaurant> favoriteRestaurants = new HashSet<>();


    public User(){}

    public User(String fullName, String phone, String email, String password, Role role, String address) {
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.password = password;
        this.role = role;
        this.address = address;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof User && ((User) o).getId().equals(id);
    }

    public void addFavoriteRestaurant(Restaurant restaurant) {
        favoriteRestaurants.add(restaurant);
    }

    public void removeFavoriteRestaurant(Restaurant restaurant) {
        favoriteRestaurants.remove(restaurant);
    }

}
