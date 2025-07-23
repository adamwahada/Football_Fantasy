package FootballFantasy.fantasy.Dto;

import FootballFantasy.fantasy.Entities.GameweekEntity.PredictionResult;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PredictionDTO {
    @NotNull(message = "Match ID is required")
    private Long matchId;

    @NotNull(message = "Predicted result is required")
    private PredictionResult predictedResult; // HOME_WIN, AWAY_WIN, DRAW

    // Optional score predictions (required only for tiebreaker matches)
    @Min(value = 0, message = "Home score must be non-negative")
    private Integer predictedHomeScore;

    @Min(value = 0, message = "Away score must be non-negative")
    private Integer predictedAwayScore;

    // Helper methods for validation
    public boolean hasScorePrediction() {
        return predictedHomeScore != null && predictedAwayScore != null;
    }

    public boolean isValidForTiebreaker() {
        return predictedResult != null && hasScorePrediction();
    }
}
