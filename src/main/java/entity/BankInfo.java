package entity;

import jakarta.persistence.*;

@Entity
@Table(name = "BankInfo")
public class BankInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String bankName;

    @Column
    private String accountNumber;

    @OneToOne (mappedBy = "bankInfo")
    private Profile profile;


    public BankInfo() {}

    public BankInfo(String BankName, String AccountNumber) {
        this.bankName = BankName;
        this.accountNumber = AccountNumber;
    }
}
