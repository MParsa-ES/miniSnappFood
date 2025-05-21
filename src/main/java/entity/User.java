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
@Table(name = "Users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @Column(unique = false, nullable = false)
    private String name;

    @Column(unique = false, nullable = false)
    private String familyName;

    @Column(unique = true, nullable = false)
    private String phone;

    @Column(unique = false, nullable = true)
    private String email;  // optional

    @Column(unique = false, nullable = false)
    private String password;

    @Column(unique = false, nullable = false)
    private Role role;

    @Column(unique = false, nullable = false)
    private String address;




    public User(String name, String familyName, String phone, String email, String password, Role role, String address) {
        this.name = name;
        this.familyName = familyName;
        this.phone = phone;
        this.email = email;
        this.password = password;
        this.role = role;
        this.address = address;
    }
}
