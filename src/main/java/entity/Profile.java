package entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Profile")
public class Profile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String profilePicture;

    @Column (unique = false, nullable = true)
    private BankInfo bankInfo;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private Member member;

    public Profile() {
    }

    public Profile(String profilePicture, BankInfo bankInfo, Member member) {
        this.profilePicture = profilePicture;
        this.bankInfo = bankInfo;
        this.member = member;
    }
}
