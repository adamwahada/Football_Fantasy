package FootballFantasy.fantasy.Entities.UserEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class UserHistory {
    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private UserEntity user;

    private int totalSessions;
    private int wonSessions;
    private BigDecimal totalWinnings;
    private BigDecimal totalSpent;
    private double averageAccuracy;

    private LocalDateTime recordedAt;
}