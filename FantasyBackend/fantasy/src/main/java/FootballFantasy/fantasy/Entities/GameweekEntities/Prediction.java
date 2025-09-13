package FootballFantasy.fantasy.Entities.GameweekEntities;

import FootballFantasy.fantasy.Entities.UserEntities.UserEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "prediction",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "match_id", "session_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Prediction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participation_id", nullable = false)
    private SessionParticipation participation;

    // 🏆 MAIN PREDICTION (Required for all matches)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PredictionResult predictedResult; // HOME_WIN, AWAY_WIN, DRAW

    @Column(nullable = false)
    private LocalDateTime predictionTime;

    private Boolean isCorrect;

    // ⚽ SCORE PREDICTION (Optional - only for tie-breaking matches)
    @Column(name = "predicted_home_score")
    private Integer predictedHomeScore;

    @Column(name = "predicted_away_score")
    private Integer predictedAwayScore;

    // 🎯 Flag to identify tie-breaking predictions
    @Column(name = "is_tiebreaker")
    private Boolean isTiebreaker;

    // 📊 Score prediction accuracy (calculated after match)
    @Column(name = "score_distance")
    private Double scoreDistance;

    // Helper methods
    public boolean hasScorePrediction() {
        return predictedHomeScore != null && predictedAwayScore != null;
    }

    public boolean isValidTiebreaker() {
        return isTiebreaker && hasScorePrediction();
    }

    // Calculate distance from actual score
    public void calculateScoreDistance(Integer actualHomeScore, Integer actualAwayScore) {
        if (hasScorePrediction() && actualHomeScore != null && actualAwayScore != null) {
            double homeDistance = Math.pow(predictedHomeScore - actualHomeScore, 2);
            double awayDistance = Math.pow(predictedAwayScore - actualAwayScore, 2);
            this.scoreDistance = Math.sqrt(homeDistance + awayDistance);
        } else {
            this.scoreDistance = 10.0; // Maximum penalty
        }
    }
}