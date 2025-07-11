
package FootballFantasy.fantasy.Entities.GameweekEntity;

import FootballFantasy.fantasy.Entities.UserEntity.UserEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "prediction",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "match_id", "session_id"}))
@Data
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PredictionResult predictedResult;

    @Column(nullable = false)
    private LocalDateTime predictionTime;

    private Boolean isCorrect;
}