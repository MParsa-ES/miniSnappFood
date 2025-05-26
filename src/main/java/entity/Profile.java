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

    @Lob
    @Column (unique = false, nullable = true)
    private String profileImageBase64;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn (name = "bankInfo_id" , nullable = true)
    private BankInfo bank_info;

    @OneToOne(mappedBy = "profile")
    private User user;

    public Profile() {
    }

    public Profile(String profilePicture, BankInfo bankInfo, User user) {
        this.profileImageBase64 = profilePicture;
        this.bank_info = bankInfo;
        this.user = user;
    }
}
