package FootballFantasy.fantasy.Entities.PaiementEntities;

import FootballFantasy.fantasy.Entities.UserEntities.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "withdraw_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private TransactionStatus status = TransactionStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentPlatform platform;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PrefixedAmount prefixedAmount;

    @Column(nullable = false)
    private String withdrawNumber;

    @Column(nullable = false)
    private boolean reserved = false;
    private LocalDateTime reservedAt;


    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;

    private String reservedByKeycloakId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity requester;


}
