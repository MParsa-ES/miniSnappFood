package entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "BankInfo")
@Getter
@Setter
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
