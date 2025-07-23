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
    private SessionParticipationService sessionParticipationService;

    // 🆕 New combined endpoint: Create session + submit predictions
    @PostMapping("/submit-predictions")
    public ResponseEntity<?> submitPredictionsAndJoinSession(
            @Valid @RequestBody GameweekPredictionSubmissionDTO submissionDTO,
            @RequestParam SessionType sessionType,
            @RequestParam BigDecimal buyInAmount,
            @RequestParam boolean isPrivate,
            @RequestParam(required = false) String accessKey) {

        try {
            Map<String, Object> result = predictionService.submitPredictionsAndJoinSession(
                    submissionDTO, sessionType, buyInAmount, isPrivate, accessKey);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Predictions submitted and session joined successfully!",
                    "data", result
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
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
