    package entity;

    import jakarta.persistence.*;
    import lombok.Getter;
    import lombok.Setter;
    import org.hibernate.annotations.CreationTimestamp;
    import org.hibernate.annotations.UpdateTimestamp;

    import java.math.BigDecimal;
    import java.time.LocalDateTime;
    import java.util.ArrayList;
    import java.util.List;

    @Entity
    @Table(name = "Orders")
    @Getter
    @Setter
    public class Order {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(nullable = false)
        private String deliveryAddress;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "customer_id", nullable = false)
        private User customer;

        // TODO: Add Coupon id later

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "restaurant_id", nullable = false)
        private Restaurant restaurant;

        @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
        private List<OrderItem> items = new ArrayList<>();


        @Column(precision = 10, scale = 2, nullable = false)
        private BigDecimal rawPrice;

        @Column(precision = 10, scale = 2, nullable = false)
        private BigDecimal taxFee;

        @Column(precision = 10, scale = 2, nullable = false)
        private BigDecimal additionalFee;

        // TODO: add courier fee later

        @Column(precision = 10, scale = 2, nullable = false)
        private BigDecimal totalPrice;



        // TODO: add courier id

        @Enumerated(EnumType.STRING)
        @Column(nullable = false)
        private OrderStatus status;

        @CreationTimestamp
        @Column(nullable = false, updatable = false)
        private LocalDateTime createdAt;

        @UpdateTimestamp
        @Column(nullable = false)
        private LocalDateTime updatedAt;

    }