package entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

@Entity
@Getter
@Setter
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = false, nullable = false)
    private String name;

    @Column(unique = false, nullable = false)
    private String address;

    @Column(unique = false, nullable = false)
    private String phone;

    @Column(unique = false, nullable = true)
    private String logo;

    @Column(unique = false, nullable = true)
    private int taxFee;

    @Column(unique = false, nullable = true)
    private int additionalFee;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "menu_id")
    private Menu menu;
    @OneToOne
    @JoinColumn(name = "owner_id")
    private User owner;



}
