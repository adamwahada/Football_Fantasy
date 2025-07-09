package FootballFantasy.fantasy.Entities.GameweekEntity;
import FootballFantasy.fantasy.Entities.UserEntity.UserEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "session_participation",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "session_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionParticipation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer totalCorrectPredictions = 0;

    @Column(nullable = false)
    private Integer totalPredictions = 0;

    @Column(nullable = false)
    private Double accuracyPercentage = 0.0;

    @Column(nullable = false)
    private LocalDateTime joinedAt;

    @Column(nullable = false)
    private BigDecimal amountPaid;

    // Ranking within this session
    private Integer rank;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipationStatus status = ParticipationStatus.ACTIVE;

    @Column(nullable = false)
    private Boolean hasCompletedAllPredictions = false;

    // Prize won (if any)
    private BigDecimal prizeWon = BigDecimal.ZERO;

    // User and session references
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private CompetitionSession session;

    // Link to user's predictions for this session
    @JsonIgnore
    @OneToMany(mappedBy = "participation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Prediction> predictions = new ArrayList<>();
}