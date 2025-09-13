package FootballFantasy.fantasy.Controller.GameweekController;

import FootballFantasy.fantasy.Entities.GameweekEntities.CompetitionSession;
import FootballFantasy.fantasy.Entities.GameweekEntities.LeagueTheme;
import FootballFantasy.fantasy.Entities.GameweekEntities.SessionParticipation;
import FootballFantasy.fantasy.Entities.GameweekEntities.SessionType;
import FootballFantasy.fantasy.Services.GameweekService.CompetitionSessionService;
import FootballFantasy.fantasy.Services.GameweekService.SessionParticipationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/competition-sessions")
public class CompetitionSessionController {

    @Autowired
    private CompetitionSessionService competitionSessionService;

    @Autowired
    private SessionParticipationService sessionParticipationService;

    // ✅ 3. Get session details with participants and current status
    @GetMapping("/{sessionId}")
    public ResponseEntity<CompetitionSession> getSession(@PathVariable Long sessionId) {
        // You'll need to add this method to CompetitionSessionService
        CompetitionSession session = competitionSessionService.getSessionById(sessionId);
        return ResponseEntity.ok(session);
    }

    // ✅ 4. Get user's participations for a specific gameweek
    @GetMapping("/user/{userId}/gameweek/{gameweekId}")
    public ResponseEntity<List<SessionParticipation>> getUserParticipationsForGameweek(
            @PathVariable Long userId,
            @PathVariable Long gameweekId) {

        List<SessionParticipation> participations = sessionParticipationService
                .getUserParticipationsForGameweek(userId, gameweekId);
        return ResponseEntity.ok(participations);
    }

    // ✅ 5. Get user participations by Keycloak ID
    @GetMapping("/user-keycloak/{keycloakId}/gameweek/{gameweekId}")
    public ResponseEntity<List<SessionParticipation>> getUserParticipationsByKeycloak(
            @PathVariable String keycloakId,
            @PathVariable Long gameweekId) {

        List<SessionParticipation> participations = sessionParticipationService
                .getUserParticipationsForGameweekByKeycloakId(keycloakId, gameweekId);
        return ResponseEntity.ok(participations);
    }

    // ✅ 6. Check if user can join a specific session type (prevent duplicates)
    @GetMapping("/can-join")
    public ResponseEntity<Boolean> canUserJoinSession(
            @RequestParam Long userId,
            @RequestParam Long gameweekId,
            @RequestParam SessionType sessionType,
            @RequestParam BigDecimal buyInAmount,
            @RequestParam LeagueTheme competition) {

        boolean canJoin = sessionParticipationService.canUserJoinSession(
                userId, gameweekId, sessionType, buyInAmount, competition);
        return ResponseEntity.ok(canJoin);
    }

    // ✅ 7. Get session leaderboard/rankings (during or after session)
    @GetMapping("/{sessionId}/leaderboard")
    public ResponseEntity<List<SessionParticipation>> getSessionLeaderboard(@PathVariable Long sessionId) {
        List<SessionParticipation> leaderboard = sessionParticipationService.getSessionParticipations(sessionId);
        return ResponseEntity.ok(leaderboard);
    }

    // ✅ 8. Get prize breakdown for a session (what user can win)
    @GetMapping("/{sessionId}/prize-breakdown")
    public ResponseEntity<CompetitionSessionService.SessionPrizeBreakdown> getPrizeBreakdown(
            @PathVariable Long sessionId) {

        CompetitionSessionService.SessionPrizeBreakdown breakdown =
                competitionSessionService.getSessionPrizeBreakdown(sessionId);
        return ResponseEntity.ok(breakdown);
    }

    // ✅ 9. Get user's active sessions (sessions they're currently in)
    @GetMapping("/user/{userId}/active")
    public ResponseEntity<List<SessionParticipation>> getUserActiveSessions(@PathVariable Long userId) {
        List<SessionParticipation> activeSessions = sessionParticipationService
                .getUserActiveParticipations(userId);
        return ResponseEntity.ok(activeSessions);
    }

    // ✅ 10. Get user's active sessions by Keycloak ID
    @GetMapping("/user-keycloak/{keycloakId}/active")
    public ResponseEntity<List<SessionParticipation>> getUserActiveSessionsByKeycloak(
            @PathVariable String keycloakId) {

        List<SessionParticipation> activeSessions = sessionParticipationService
                .getUserActiveParticipationsByKeycloakId(keycloakId);
        return ResponseEntity.ok(activeSessions);
    }

    // ✅ 11. Admin: Manually trigger winner determination for a GameWeek
    @PostMapping("/admin/manual-trigger/gameweek/{gameWeekId}")
    public ResponseEntity<String> manuallyTriggerWinners(@PathVariable Long gameWeekId) {
        competitionSessionService.manuallyTriggerWinnerDetermination(gameWeekId);
        return ResponseEntity.ok("✅ Manual winner determination triggered for GameWeek ID: " + gameWeekId);
    }

    // ✅ 12. Admin: Force winner calculation for one session (fallback)
    @PostMapping("/admin/manual-trigger/session/{sessionId}")
    public ResponseEntity<String> triggerWinnerForSession(@PathVariable Long sessionId) {
        competitionSessionService.determineWinner(sessionId);
        return ResponseEntity.ok("✅ Winner calculated for session ID: " + sessionId);
    }

    // ✅ 13. Admin: Automatically determine winners for completed gameweek
    @PostMapping("/admin/auto-determine/gameweek/{gameWeekId}")
    public ResponseEntity<String> autoTriggerWinners(@PathVariable Long gameWeekId) {
        competitionSessionService.determineWinnersForCompletedGameWeek(gameWeekId);
        return ResponseEntity.ok("✅ Automatic winner determination completed for GameWeek ID: " + gameWeekId);
    }
}