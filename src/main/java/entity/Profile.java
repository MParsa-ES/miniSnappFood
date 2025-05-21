package entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "Profile")
public class Profile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column (unique = false, nullable = false)
    private String profilePicture;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn (name = "bankInfo_id" , nullable = true)
    private BankInfo bankInfo;

    @OneToOne(mappedBy = "profile")
    private User user;

    public Profile() {
    }

    public Profile(String profilePicture, BankInfo bankInfo, User user) {
        this.profilePicture = profilePicture;
        this.bankInfo = bankInfo;
        this.user = user;
    }
}
