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
    private String bank_name;

    @Column
    private String account_number;

    @OneToOne (mappedBy = "bank_info")
    private Profile profile;


    public BankInfo() {}

    public BankInfo(String BankName, String AccountNumber) {
        this.bank_name = BankName;
        this.account_number = AccountNumber;
    }
}
