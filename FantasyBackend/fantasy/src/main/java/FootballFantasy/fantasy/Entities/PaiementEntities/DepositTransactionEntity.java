package FootballFantasy.fantasy.Entities.PaiementEntities;

import FootballFantasy.fantasy.Entities.UserEntities.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "deposit_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepositTransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity depositor;

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

    private String screenshotUrl;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "approved_by_admin_id")
    private UserEntity approvedBy;

    @ManyToOne
    @JoinColumn(name = "withdraw_id")
    private WithdrawRequestEntity matchedWithdraw;

}
