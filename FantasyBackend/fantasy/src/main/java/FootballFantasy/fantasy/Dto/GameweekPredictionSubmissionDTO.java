package FootballFantasy.fantasy.Dto;

import FootballFantasy.fantasy.Entities.GameweekEntity.LeagueTheme;
import FootballFantasy.fantasy.Entities.GameweekEntity.SessionType;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameweekPredictionSubmissionDTO {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Gameweek ID is required")
    private Long gameweekId;

    @NotNull(message = "competition ")
    private LeagueTheme competition;

    @NotNull(message = "Predictions are required")
    @Valid
    private List<PredictionDTO> predictions;

    // ➕ New fields to allow session creation during submission
    @NotNull(message = "Session type is required")
    private SessionType sessionType;

    @NotNull(message = "Buy-in amount is required")
    private BigDecimal buyInAmount;

    private Long sessionId;

    private boolean isPrivate = false;

    private String sessionDescription; // optional

    // Optional list of match IDs to be used for tie-breaker (e.g. 1 or 2 matches)
    private List<Long> tiebreakerMatchIds;

    // Optional: whether the prediction is finalized
    private boolean complete;

    // Validation helper (updated)
    public boolean isComplete() {
        return userId != null &&
                gameweekId != null &&
                predictions != null && !predictions.isEmpty() &&
                sessionType != null &&
                buyInAmount != null;
    }
}
