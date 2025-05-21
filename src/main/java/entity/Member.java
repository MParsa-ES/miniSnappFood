package entity;

import jakarta.persistence.*;

enum Role {
    BUYER, SELLER, COURIER
}


@Entity
@Getter
@Setter
@Table(name = "Users")
public class Member {
    private String name;
    private String familyName;
    private String phone;
    private String email;  // optional
    private String password;
    private Role role;
    private String address;

}
