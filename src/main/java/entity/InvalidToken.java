package entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Entity
@Table(name = "InvalidTokens")
@Getter
@Setter
@NoArgsConstructor
public class InvalidToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String jti; // JWT ID

    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date expiryDate;

    public InvalidToken(String jti, Date expiryDate) {
        this.jti = jti;
        this.expiryDate = expiryDate;
    }
}