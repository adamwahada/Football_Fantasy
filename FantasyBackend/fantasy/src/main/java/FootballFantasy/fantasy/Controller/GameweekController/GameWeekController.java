package FootballFantasy.fantasy.Controller.GameweekController;

import FootballFantasy.fantasy.Entities.GameweekEntity.GameWeek;
import FootballFantasy.fantasy.Entities.GameweekEntity.LeagueTheme;
import FootballFantasy.fantasy.Entities.GameweekEntity.Match;
import FootballFantasy.fantasy.Services.GameweekService.GameWeekService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gameweeks")
public class GameWeekController {

    @Autowired
    private GameWeekService gameWeekService;

    // ✅ Create a new GameWeek
    @PostMapping
//    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<GameWeek> createGameWeek(@RequestBody GameWeek gameWeek) {
        GameWeek saved = gameWeekService.createGameWeek(gameWeek);
        return ResponseEntity.ok(saved);
    }
    // ✅ update a GameWeek

    @PutMapping("/{gameWeekId}")
    public ResponseEntity<GameWeek> updateGameWeek(@PathVariable Long gameWeekId, @RequestBody GameWeek gameWeek) {
        GameWeek updated = gameWeekService.updateGameWeek(gameWeekId, gameWeek);
        return ResponseEntity.ok(updated);
    }
    // ✅ delete a GameWeek

    @DeleteMapping("/{gameWeekId}")
    public ResponseEntity<Void> deleteGameWeek(@PathVariable Long gameWeekId) {
        gameWeekService.deleteGameWeek(gameWeekId);
        return ResponseEntity.noContent().build(); // 204 No Content
    }


    // ✅ Add a Match to a GameWeek
    @PostMapping("/{gameWeekId}/matches")
    public ResponseEntity<Match> addMatchToGameWeek(@PathVariable Long gameWeekId, @RequestBody Match match) {
        Match savedMatch = gameWeekService.addMatchToGameWeek(gameWeekId, match);
        return ResponseEntity.ok(savedMatch);
    }

    // ✅ Get Matches for a specific GameWeek
    @GetMapping("/{gameWeekId}/matches")
    public ResponseEntity<List<Match>> getMatchesByGameWeek(@PathVariable Long gameWeekId) {
        List<Match> matches = gameWeekService.getMatchesByGameWeek(gameWeekId);
        return ResponseEntity.ok(matches);
    }

    @DeleteMapping("/{gameWeekId}/matches")
    public ResponseEntity<Void> deleteAllMatchesForGameWeek(@PathVariable Long gameWeekId) {
        gameWeekService.deleteMatchesByGameWeek(gameWeekId);
        return ResponseEntity.noContent().build();
    }
    @DeleteMapping("/{gameWeekId}/RemoveMatches")
    public ResponseEntity<Void> deleteMatchesFromGameWeek(
            @PathVariable Long gameWeekId,
            @RequestBody List<Long> matchIdsToRemove) {

        gameWeekService.deleteSpecificMatchesFromGameWeek(gameWeekId, matchIdsToRemove);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{gameWeekId}/matches/{matchId}")
    public ResponseEntity<Match> linkExistingMatchToGameWeek(
            @PathVariable Long gameWeekId,
            @PathVariable Long matchId) {
        Match updated = gameWeekService.linkExistingMatchToGameWeek(gameWeekId, matchId);
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{gameWeekId}/matches")
    public ResponseEntity<List<Match>> linkMultipleMatchesToGameWeek(
            @PathVariable Long gameWeekId,
            @RequestBody List<Long> matchIds) {
        List<Match> updated = gameWeekService.linkMultipleMatchesToGameWeek(gameWeekId, matchIds);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/week/{weekNumber}")
    public ResponseEntity<GameWeek> getByWeekNumber(@PathVariable int weekNumber) {
        GameWeek gameWeek = gameWeekService.getByWeekNumber(weekNumber);
        return ResponseEntity.ok(gameWeek);
    }

    // ✅ Check if all matches in a GameWeek are completed
    @GetMapping("/{gameWeekId}/is-complete")
    public ResponseEntity<Boolean> isGameWeekComplete(@PathVariable Long gameWeekId) {
        boolean complete = gameWeekService.isGameWeekComplete(gameWeekId);
        return ResponseEntity.ok(complete);
    }

    @GetMapping("/theme/{theme}")
    public List<GameWeek> getByCompetition(@PathVariable LeagueTheme Competition) {
        return gameWeekService.getGameWeeksByCompetition(Competition);
    }

    // Import multiple matches and link them to a specific GameWeek
    @PostMapping("/{gameWeekId}/import-matches")
    public ResponseEntity<String> importMatchesToGameWeek(
            @PathVariable Long gameWeekId,
            @RequestBody List<Match> matches // Expecting a JSON array of Match objects
    ) {
        try {
            gameWeekService.importMatchesToGameWeek(gameWeekId, matches);
            return ResponseEntity.ok("Matches imported and linked to GameWeek successfully.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Unexpected error occurred.");
        }
    }

}
