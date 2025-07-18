package FootballFantasy.fantasy.Controller.GameweekController;

import FootballFantasy.fantasy.Entities.GameweekEntity.CompetitionSession;
import FootballFantasy.fantasy.Entities.GameweekEntity.LeagueTheme;
import FootballFantasy.fantasy.Entities.GameweekEntity.SessionType;
import FootballFantasy.fantasy.Services.GameweekService.CompetitionSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/competition-sessions")
public class CompetitionSessionController {

    @Autowired
    private CompetitionSessionService competitionSessionService;

    // ✅ 1. Join or Create a session (for users)
    @PostMapping("/join")
    public ResponseEntity<CompetitionSession> joinSession(
            @RequestParam Long gameweekId,
            @RequestParam LeagueTheme competition,
            @RequestParam SessionType sessionType,
            @RequestParam BigDecimal buyInAmount,
            @RequestParam boolean isPrivate,
            @RequestParam(required = false) String accessKey,
            @RequestParam Long userId) {

        CompetitionSession session = competitionSessionService.joinOrCreateSession(
                gameweekId,
                sessionType,
                buyInAmount,
                isPrivate,
                accessKey,
                competition
        );
        return ResponseEntity.ok(session);
    }

    // ✅ 2. Admin: Manually trigger winner determination for a GameWeek
    @PostMapping("/admin/manual-trigger/gameweek/{gameWeekId}")
    public ResponseEntity<String> manuallyTriggerWinners(@PathVariable Long gameWeekId) {
        competitionSessionService.manuallyTriggerWinnerDetermination(gameWeekId);
        return ResponseEntity.ok("✅ Manual winner determination triggered for GameWeek ID: " + gameWeekId);
    }

    // ✅ 3. Admin: Force winner calculation for one session (fallback)
    @PostMapping("/admin/manual-trigger/session/{sessionId}")
    public ResponseEntity<String> triggerWinnerForSession(@PathVariable Long sessionId) {
        competitionSessionService.determineWinner(sessionId);
        return ResponseEntity.ok("✅ Winner calculated for session ID: " + sessionId);
    }
}
