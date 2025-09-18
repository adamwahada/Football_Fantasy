package FootballFantasy.fantasy.Controller.GameweekControllers;

import FootballFantasy.fantasy.Dto.UserSessionStats;
import FootballFantasy.fantasy.Entities.GameweekEntities.LeagueTheme;
import FootballFantasy.fantasy.Entities.GameweekEntities.SessionParticipation;
import FootballFantasy.fantasy.Entities.GameweekEntities.SessionType;
import FootballFantasy.fantasy.Services.GameweekService.SessionParticipationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/session-participation")
@Tag(name = "Session Participation", description = "API for managing session participation")
public class SessionParticipationController {

    @Autowired
    private SessionParticipationService sessionParticipationService;

    // ===== JOIN COMPETITION/SESSION =====

    @PostMapping("/join-competition")
    @Operation(summary = "Join or create a competition session")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully joined competition"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "403", description = "User is banned"),
            @ApiResponse(responseCode = "409", description = "User already in session or session full"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> joinCompetition(
            @Parameter(description = "Gameweek ID") @RequestParam Long gameweekId,
            @Parameter(description = "League theme/competition") @RequestParam LeagueTheme competition,
            @Parameter(description = "Session type") @RequestParam SessionType sessionType,
            @Parameter(description = "Buy-in amount") @RequestParam BigDecimal buyInAmount,
            @Parameter(description = "Is private session") @RequestParam(defaultValue = "false") boolean isPrivate,
            @Parameter(description = "Access key for private sessions") @RequestParam(required = false) String accessKey,
            @Parameter(description = "Private mode: CREATE or JOIN") @RequestParam(required = false) String privateMode) {

        try {
            String keycloakId = getCurrentUserKeycloakId();

            System.out.println("🔍 [JOIN-COMP CONTROLLER] Received params:" +
                    "\n- keycloakId: " + keycloakId +
                    "\n- gameweekId: " + gameweekId +
                    "\n- competition: " + competition +
                    "\n- sessionType: " + sessionType +
                    "\n- buyInAmount: " + buyInAmount +
                    "\n- isPrivate: " + isPrivate +
                    "\n- accessKey: '" + accessKey + "'" +
                    "\n- privateMode: '" + privateMode + "'");

            SessionParticipation participation = sessionParticipationService.joinCompetitionByKeycloakId(
                    gameweekId, competition, sessionType, buyInAmount, isPrivate, accessKey, keycloakId, privateMode);

            return ResponseEntity.ok(participation);
        } catch (Exception e) {
            System.err.println("❌ [CONTROLLER] Error joining competition: " + e.getMessage());
            
            // Return proper error message instead of "Unexpected error"
            if (e.getMessage() != null && e.getMessage().contains("Aucune session privée trouvée")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "PRIVATE_SESSION_NOT_FOUND", "message", "Aucune session privée trouvée avec cette clé d'accès"));
            } else if (e.getMessage() != null && e.getMessage().contains("Access key is required")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "ACCESS_KEY_REQUIRED", "message", "Clé d'accès requise pour rejoindre une session privée"));
            } else if (e.getMessage() != null && e.getMessage().contains("Mismatch between GameWeek")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "COMPETITION_MISMATCH", "message", "La compétition ne correspond pas à cette gameweek"));
            } else if (e.getMessage() != null && e.getMessage().contains("Cette clé d'accès est pour")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "PRIVATE_SESSION_GAMEWEEK_MISMATCH", "message", e.getMessage()));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "INTERNAL_ERROR", "message", "Erreur interne: " + e.getMessage()));
            }
        }
    }



    @PostMapping("/join-session/{sessionId}")
    @Operation(summary = "Join an existing session")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully joined session"),
            @ApiResponse(responseCode = "404", description = "Session not found"),
            @ApiResponse(responseCode = "409", description = "User already in session or session full"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> joinSession(
            @Parameter(description = "Session ID") @PathVariable Long sessionId) {

        try {
            String keycloakId = getCurrentUserKeycloakId();

            SessionParticipation participation = sessionParticipationService.joinSessionByKeycloakId(
                    sessionId, keycloakId);

            return ResponseEntity.ok(participation);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to join session: " + e.getMessage());
        }
    }

    // ===== LEAVE SESSION =====

    @DeleteMapping("/leave-session/{sessionId}")
    @Operation(summary = "Leave a session")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully left session"),
            @ApiResponse(responseCode = "404", description = "Session or participation not found"),
            @ApiResponse(responseCode = "409", description = "Cannot leave session - already started"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> leaveSession(
            @Parameter(description = "Session ID") @PathVariable Long sessionId) {

        try {
            String keycloakId = getCurrentUserKeycloakId();

            sessionParticipationService.leaveSessionByKeycloakId(sessionId, keycloakId);

            return ResponseEntity.ok("Successfully left session");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to leave session: " + e.getMessage());
        }
    }

    // ===== UPDATE PREDICTION PROGRESS =====

    @PutMapping("/update-prediction-progress/{participationId}")
    @Operation(summary = "Update user's prediction progress")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully updated prediction progress"),
            @ApiResponse(responseCode = "404", description = "Participation not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> updatePredictionProgress(
            @Parameter(description = "Participation ID") @PathVariable Long participationId,
            @Parameter(description = "Correct predictions count") @RequestParam int correctPredictions,
            @Parameter(description = "Total predictions count") @RequestParam int totalPredictions,
            @Parameter(description = "Has completed all predictions") @RequestParam boolean hasCompletedAll) {

        try {
            sessionParticipationService.updatePredictionProgress(
                    participationId, correctPredictions, totalPredictions, hasCompletedAll);

            return ResponseEntity.ok("Prediction progress updated successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to update prediction progress: " + e.getMessage());
        }
    }

    // ===== GET USER PARTICIPATION =====

    @GetMapping("/user-participation/{sessionId}")
    @Operation(summary = "Get user's participation in a specific session")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved participation"),
            @ApiResponse(responseCode = "404", description = "Participation not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getUserParticipation(
            @Parameter(description = "Session ID") @PathVariable Long sessionId) {

        try {
            String keycloakId = getCurrentUserKeycloakId();

            Optional<SessionParticipation> participation = sessionParticipationService
                    .getUserParticipationByKeycloakId(keycloakId, sessionId);

            if (participation.isPresent()) {
                return ResponseEntity.ok(participation.get());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("User participation not found for session");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to retrieve participation: " + e.getMessage());
        }
    }

    @GetMapping("/user-participations/gameweek/{gameweekId}")
    @Operation(summary = "Get user's participations for a gameweek")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved participations"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getUserParticipationsForGameweek(
            @Parameter(description = "Gameweek ID") @PathVariable Long gameweekId) {

        try {
            String keycloakId = getCurrentUserKeycloakId();

            List<SessionParticipation> participations = sessionParticipationService
                    .getUserParticipationsForGameweekByKeycloakId(keycloakId, gameweekId);

            return ResponseEntity.ok(participations);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to retrieve participations: " + e.getMessage());
        }
    }

    @GetMapping("/user-participations/active")
    @Operation(summary = "Get user's active participations")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved active participations"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getUserActiveParticipations() {

        try {
            String keycloakId = getCurrentUserKeycloakId();

            List<SessionParticipation> participations = sessionParticipationService
                    .getUserActiveParticipationsByKeycloakId(keycloakId);

            return ResponseEntity.ok(participations);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to retrieve active participations: " + e.getMessage());
        }
    }

    // ===== GET SESSION PARTICIPATIONS =====

    @GetMapping("/session-participations/{sessionId}")
    @Operation(summary = "Get all participations for a session")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved session participations"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getSessionParticipations(
            @Parameter(description = "Session ID") @PathVariable Long sessionId) {

        try {
            List<SessionParticipation> participations = sessionParticipationService
                    .getSessionParticipations(sessionId);

            return ResponseEntity.ok(participations);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to retrieve session participations: " + e.getMessage());
        }
    }

    // ===== CHECK USER ELIGIBILITY =====

    @GetMapping("/can-join-session")
    @Operation(summary = "Check if user can join a session type for a gameweek")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully checked eligibility"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> canUserJoinSession(
            @Parameter(description = "Gameweek ID") @RequestParam Long gameweekId,
            @Parameter(description = "Session type") @RequestParam SessionType sessionType,
            @Parameter(description = "Buy-in amount") @RequestParam BigDecimal buyInAmount,
            @Parameter(description = "competition") @RequestParam LeagueTheme competition

    ) {

        try {
            String keycloakId = getCurrentUserKeycloakId();

            boolean canJoin = sessionParticipationService.canUserJoinSessionByKeycloakId(
                    keycloakId, gameweekId, sessionType, buyInAmount,competition);

            return ResponseEntity.ok(new EligibilityResponse(canJoin));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to check eligibility: " + e.getMessage());
        }
    }

    // ===== USER STATISTICS =====

    @GetMapping("/user-stats")
    @Operation(summary = "Get user's session statistics")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved user statistics"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getUserSessionStats() {

        try {
            String keycloakId = getCurrentUserKeycloakId();

            UserSessionStats stats = sessionParticipationService
                    .getUserSessionStatsByKeycloakId(keycloakId);

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to retrieve user statistics: " + e.getMessage());
        }
    }

    @GetMapping("/user-total-winnings")
    @Operation(summary = "Get user's total winnings")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved total winnings"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getUserTotalWinnings() {

        try {
            String keycloakId = getCurrentUserKeycloakId();

            BigDecimal totalWinnings = sessionParticipationService
                    .calculateUserTotalWinningsByKeycloakId(keycloakId);

            return ResponseEntity.ok(new WinningsResponse(totalWinnings));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to retrieve total winnings: " + e.getMessage());
        }
    }

    // ===== ADMIN ENDPOINTS =====

    @GetMapping("/admin/user-stats/{userId}")
    @Operation(summary = "Get user's session statistics (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved user statistics"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getUserSessionStatsAdmin(
            @Parameter(description = "User ID") @PathVariable Long userId) {

        try {
            UserSessionStats stats = sessionParticipationService.getUserSessionStats(userId);
            return ResponseEntity.ok(stats);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to retrieve user statistics: " + e.getMessage());
        }
    }

    // ===== HELPER METHODS =====

    private String getCurrentUserKeycloakId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null) {
            return authentication.getName(); // Keycloak ID from JWT
        }
        throw new RuntimeException("User not authenticated");
    }

    // ===== RESPONSE DTOs =====

    public static class EligibilityResponse {
        private boolean canJoin;

        public EligibilityResponse(boolean canJoin) {
            this.canJoin = canJoin;
        }

        public boolean isCanJoin() {
            return canJoin;
        }

        public void setCanJoin(boolean canJoin) {
            this.canJoin = canJoin;
        }
    }

    public static class WinningsResponse {
        private BigDecimal totalWinnings;

        public WinningsResponse(BigDecimal totalWinnings) {
            this.totalWinnings = totalWinnings;
        }

        public BigDecimal getTotalWinnings() {
            return totalWinnings;
        }

        public void setTotalWinnings(BigDecimal totalWinnings) {
            this.totalWinnings = totalWinnings;
        }
    }
}