package FootballFantasy.fantasy.Controller.GameweekControllers;

import FootballFantasy.fantasy.Dto.GameweekPredictionSubmissionDTO;
import FootballFantasy.fantasy.Entities.GameweekEntities.*;
import FootballFantasy.fantasy.Exception.BusinessLogicException;
import FootballFantasy.fantasy.Exception.InsufficientBalanceException;
import FootballFantasy.fantasy.Exception.PrivateSessionNotFoundException;
import FootballFantasy.fantasy.Exception.PrivateSessionGameweekMismatchException;
import FootballFantasy.fantasy.Exception.PrivateSessionFullException;
import FootballFantasy.fantasy.Services.GameweekService.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
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

    @PostMapping("/submit-predictions")
    public ResponseEntity<Map<String, Object>> submitPredictionsAndJoinSession(
            @Valid @RequestBody GameweekPredictionSubmissionDTO submissionDTO,
            @RequestParam SessionType sessionType,
            @RequestParam BigDecimal buyInAmount,
            @RequestParam boolean isPrivate,
            @RequestParam(required = false) String accessKey,
            @RequestParam(required = false) String privateMode) {

        System.out.println("🔍 [CONTROLLER] Received submission for user: " + submissionDTO.getUserId());
        System.out.println("- Gameweek: " + submissionDTO.getGameweekId());
        System.out.println("- Buy-in: " + buyInAmount);
        System.out.println("- Session type: " + sessionType);
        System.out.println("- isPrivate: " + isPrivate + ", accessKey='" + accessKey + "', privateMode='" + privateMode + "'");

        try {
            // Basic validation
            validateSubmissionRequest(submissionDTO);

            // Call service to submit predictions and join session
            Map<String, Object> result = predictionService.submitPredictionsAndJoinSession(
                    submissionDTO, sessionType, buyInAmount, isPrivate, accessKey, privateMode);

            // Return success response
            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("success", true);
            successResponse.put("message", "Predictions submitted and session joined successfully!");
            successResponse.put("data", result);
            successResponse.put("timestamp", LocalDateTime.now());

            System.out.println("✅ [CONTROLLER] Successfully processed submission for user: " + submissionDTO.getUserId());
            return ResponseEntity.ok(successResponse);

        } catch (InsufficientBalanceException ex) {
            System.out.println("❌ [CONTROLLER] Insufficient balance for user: " + submissionDTO.getUserId());
            // Re-throw to let GlobalExceptionHandler handle it with proper error structure
            throw ex;

        } catch (BusinessLogicException ex) {
            System.out.println("❌ [CONTROLLER] Business logic error: " + ex.getMessage());
            // Re-throw to let GlobalExceptionHandler handle it with proper error structure
            throw ex;

        } catch (PrivateSessionNotFoundException ex) {
            System.out.println("❌ [CONTROLLER] Private session not found: " + ex.getMessage());
            // Re-throw to let GlobalExceptionHandler handle it with proper error structure
            throw ex;

        } catch (PrivateSessionGameweekMismatchException ex) {
            System.out.println("❌ [CONTROLLER] Private session gameweek mismatch: " + ex.getMessage());
            // Re-throw to let GlobalExceptionHandler handle it with proper error structure
            throw ex;

        } catch (PrivateSessionFullException ex) {
            System.out.println("❌ [CONTROLLER] Private session full: " + ex.getMessage());
            // Re-throw to let GlobalExceptionHandler handle it with proper error structure
            throw ex;

        } catch (Exception ex) {
            System.err.println("❌ [CONTROLLER] Unexpected error: " + ex.getMessage());
            ex.printStackTrace();
            // Re-throw as BusinessLogicException to ensure consistent error handling
            throw new BusinessLogicException("Une erreur inattendue s'est produite lors du traitement de votre demande", "UNEXPECTED_ERROR");
        }
    }

    /**
     * Validate the submission request
     */
    private void validateSubmissionRequest(GameweekPredictionSubmissionDTO submissionDTO) {
        if (submissionDTO.getUserId() == null) {
            throw new BusinessLogicException("User ID is required", "USER_ID_REQUIRED");
        }
        if (submissionDTO.getGameweekId() == null) {
            throw new BusinessLogicException("Gameweek ID is required", "GAMEWEEK_ID_REQUIRED");
        }
        if (submissionDTO.getCompetition() == null) {
            throw new BusinessLogicException("Competition is required", "COMPETITION_REQUIRED");
        }
        if (submissionDTO.getPredictions() == null || submissionDTO.getPredictions().isEmpty()) {
            throw new BusinessLogicException("Predictions are required", "PREDICTIONS_REQUIRED");
        }
    }

    /**
     * Get user's predictions for a session
     */
    @GetMapping("/user/{participationId}")
    public ResponseEntity<Map<String, Object>> getUserPredictions(@PathVariable Long participationId) {
        try {
            List<Prediction> predictions = predictionService.getUserPredictionsForSession(participationId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("predictions", predictions);
            response.put("totalPredictions", predictions.size());
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            System.err.println("❌ [CONTROLLER] Error getting user predictions: " + ex.getMessage());
            throw new BusinessLogicException("Failed to retrieve predictions", "PREDICTIONS_RETRIEVAL_ERROR");
        }
    }

    /**
     * Health check endpoint to test error handling
     */
    @GetMapping("/test-insufficient-balance")
    public ResponseEntity<Map<String, Object>> testInsufficientBalance() {
        throw new InsufficientBalanceException("TEST_USER", "100.00", "50.00");
    }

    /**
     * Health check endpoint to test business logic error
     */
    @GetMapping("/test-business-error")
    public ResponseEntity<Map<String, Object>> testBusinessError() {
        throw new BusinessLogicException("This is a test business logic error", "TEST_ERROR");
    }
}