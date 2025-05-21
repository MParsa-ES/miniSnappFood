package entity;

import jakarta.persistence.*;

@Entity
@Table(name = "Bank info")
public class BankInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String BankName;

    @Column
    private String AccountNumber;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private Member member;

    public BankInfo() {}

    public BankInfo(String BankName, String AccountNumber , Member member) {
        this.BankName = BankName;
        this.AccountNumber = AccountNumber;
        this.member = member;
    }
}
