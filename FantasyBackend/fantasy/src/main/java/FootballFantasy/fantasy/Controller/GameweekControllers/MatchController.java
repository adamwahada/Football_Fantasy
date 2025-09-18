package FootballFantasy.fantasy.Controller.GameweekControllers;

import FootballFantasy.fantasy.Entities.GameweekEntities.Match;
import FootballFantasy.fantasy.Entities.GameweekEntities.GameWeek;
import FootballFantasy.fantasy.Services.DataService.MatchUpdateService;
import FootballFantasy.fantasy.Services.GameweekService.MatchService;
import FootballFantasy.fantasy.Services.GameweekService.GameWeekService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    @Autowired
    private MatchService matchService;

    @Autowired
    private MatchUpdateService matchUpdateService;

    @Autowired
    private GameWeekService gameWeekService; // Add this dependency

    // ✅ Create a match and attach it to a GameWeek
    @PostMapping
    public ResponseEntity<Match> createMatch(@RequestBody Match match) {
        Match created = matchService.createMatch(match);
        return ResponseEntity.ok(created);
    }

    // ✅ Get all matches
    @GetMapping
    public ResponseEntity<List<Match>> getAllMatches() {
        return ResponseEntity.ok(matchService.getAllMatches());
    }

    // ✅ Get match by ID
    @GetMapping("/{id}")
    public ResponseEntity<Match> getMatchById(@PathVariable Long id) {
        return ResponseEntity.ok(matchService.getMatchById(id));
    }

    // ✅ Update match
    @PutMapping("/{id}")
    public ResponseEntity<Match> updateMatch(@PathVariable Long id, @RequestBody Match match) {
        return ResponseEntity.ok(matchService.updateMatch(id, match));
    }

    // ✅ Delete match
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMatch(@PathVariable Long id) {
        matchService.deleteMatch(id);
        return ResponseEntity.noContent().build();
    }

    // ✅ Retourne Vainquer du match
    @GetMapping("/{id}/winner")
    public ResponseEntity<String> getMatchWinner(@PathVariable Long id) {
        String winner = matchService.getWinner(id);
        return ResponseEntity.ok(winner);
    }

    // ✅ UPDATED: Set match active status AND recalculate gameweek dates
    @PutMapping("/{matchId}/active")
    public ResponseEntity<Map<String, Object>> setMatchActiveStatus(
            @PathVariable Long matchId,
            @RequestParam boolean active) {
        try {
            System.out.println("🎯 Controller: Setting match " + matchId + " active status to " + active);

            // ✅ This already handles ALL gameweek updates - no need to do them again
            Match updatedMatch = matchService.setMatchActiveStatus(matchId, active);

            // Prepare response with details
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Match " + matchId + " active status set to " + active);
            response.put("matchId", matchId);
            response.put("active", updatedMatch.isActive());
            response.put("gameweeksAffected", updatedMatch.getGameweeks().size());
            response.put("affectedGameweekIds", updatedMatch.getGameweeks().stream()
                    .map(GameWeek::getId)
                    .toList());

            System.out.println("✅ Controller: Successfully updated match " + matchId + " to active=" + updatedMatch.isActive());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Controller: Error updating match " + matchId + " active status: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to update match active status");
            errorResponse.put("message", e.getMessage());
            errorResponse.put("matchId", matchId);
            errorResponse.put("requestedActive", active);

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/update")
    public ResponseEntity<String> updateMatchesManually() {
        matchUpdateService.updateMatches();
        return ResponseEntity.ok("Matches updated successfully!");
    }
}