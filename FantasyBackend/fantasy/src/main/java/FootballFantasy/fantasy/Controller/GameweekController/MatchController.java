package FootballFantasy.fantasy.Controller.GameweekController;

import FootballFantasy.fantasy.Entities.GameweekEntity.Match;
import FootballFantasy.fantasy.Services.GameweekService.MatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    @Autowired
    private MatchService matchService;

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
    @PutMapping("/{matchId}/active")
    public ResponseEntity<String> setMatchActiveStatus(@PathVariable Long matchId, @RequestParam boolean active) {
        Match match = matchService.setMatchActiveStatus(matchId, active);
        return ResponseEntity.ok("✅ Match " + matchId + " active status set to " + active);
    }
}
