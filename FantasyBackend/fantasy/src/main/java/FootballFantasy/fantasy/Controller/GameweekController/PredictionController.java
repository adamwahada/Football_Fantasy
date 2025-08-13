package FootballFantasy.fantasy.Controller.GameweekController;

import FootballFantasy.fantasy.Dto.GameweekPredictionSubmissionDTO;
import FootballFantasy.fantasy.Entities.GameweekEntity.*;
import FootballFantasy.fantasy.Services.GameweekService.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/predictions")
public class PredictionController {

    @Autowired
    private PredictionService predictionService;

    @Autowired
    private CompetitionSessionService competitionSessionService;

    @Autowired
    private SessionParticipationService sessionParticipationService;

    // 🆕 FIXED: Combined endpoint with proper error handling
    @PostMapping("/submit-predictions")
    public ResponseEntity<?> submitPredictionsAndJoinSession(
            @Valid @RequestBody GameweekPredictionSubmissionDTO submissionDTO,
            @RequestParam SessionType sessionType,
            @RequestParam BigDecimal buyInAmount,
            @RequestParam boolean isPrivate,
            @RequestParam(required = false) String accessKey) {

        try {
            // Add detailed logging for debugging
            System.out.println("🔍 [CONTROLLER] Received submission:");
            System.out.println("- DTO: " + submissionDTO);
            System.out.println("- sessionType: " + sessionType);
            System.out.println("- buyInAmount: " + buyInAmount);
            System.out.println("- isPrivate: " + isPrivate);
            System.out.println("- accessKey: " + accessKey);
            System.out.println("- predictions count: " +
                    (submissionDTO.getPredictions() != null ? submissionDTO.getPredictions().size() : "null"));

            // Validate required fields
            if (submissionDTO.getUserId() == null) {
                throw new RuntimeException("User ID is required");
            }
            if (submissionDTO.getGameweekId() == null) {
                throw new RuntimeException("Gameweek ID is required");
            }
            if (submissionDTO.getCompetition() == null) {
                throw new RuntimeException("Competition is required");
            }
            if (submissionDTO.getPredictions() == null || submissionDTO.getPredictions().isEmpty()) {
                throw new RuntimeException("Predictions are required");
            }

            Map<String, Object> result = predictionService.submitPredictionsAndJoinSession(
                    submissionDTO, sessionType, buyInAmount, isPrivate, accessKey);

            // FIXED: Return the structure your frontend expects
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Predictions submitted and session joined successfully!",
                    "predictions", result.get("predictions"),
                    "sessionParticipation", result.get("sessionParticipation")
            ));

        } catch (RuntimeException e) {
            System.err.println("❌ [CONTROLLER] Error: " + e.getMessage());
            e.printStackTrace();

            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        } catch (Exception e) {
            System.err.println("❌ [CONTROLLER] Unexpected error: " + e.getMessage());
            e.printStackTrace();

            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Internal server error: " + e.getMessage()
            ));
        }
    }

    // 🔍 Get user's predictions for a session
    @GetMapping("/user/{participationId}")
    public ResponseEntity<?> getUserPredictions(@PathVariable Long participationId) {
        try {
            List<Prediction> predictions = predictionService.getUserPredictionsForSession(participationId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "predictions", predictions,
                    "totalPredictions", predictions.size()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
}