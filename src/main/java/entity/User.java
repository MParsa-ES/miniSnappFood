package entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

enum Role {
    BUYER, SELLER, COURIER
}


@Entity
@Getter
@Setter
@Table(name = "User")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = false, nullable = false)
    private String fullName;

    @Column(unique = true, nullable = false)
    private String phone;

    @Column(unique = false, nullable = true)
    private String email;  // optional

    @Column(unique = false, nullable = false)
    private String password;

    @Enumerated(EnumType.STRING) // Store enum as a readable string
    @Column(nullable = true)
    private Role role;


    @Column(unique = false, nullable = false)
    private String address;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "profile_id" , nullable = true)
    private Profile profile;

    public User(){}


    public User(String name, String familyName, String phone, String email, String password, Role role, String address) {
        this.fullName = name;
        this.phone = phone;
        this.email = email;
        this.password = password;
        this.role = role;
        this.address = address;
    }
}
