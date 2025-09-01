package FootballFantasy.fantasy.Controller.ControllerUser;

import FootballFantasy.fantasy.Entities.GameweekEntity.LeagueTheme;
import FootballFantasy.fantasy.Services.UserService.UserService;
import FootballFantasy.fantasy.Services.DataService.MatchUpdateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private MatchUpdateService matchUpdateService;

    // ✅ Credit user balance
//    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/users/{userId}/credit")
    public ResponseEntity<String> credit(@PathVariable Long userId,
                                         @RequestParam BigDecimal amount) {
        userService.creditBalance(userId, amount);
        return ResponseEntity.ok("Balance updated");
    }

    // ✅ Manual trigger for match updates
//    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/matches/update-now")
    public ResponseEntity<String> triggerMatchUpdate(
            @RequestParam(required = false) String competition) {

        if (competition != null && !competition.isEmpty()) {
            matchUpdateService.updateMatchesManually(competition.toUpperCase());
            return ResponseEntity.ok("✅ Match update triggered manually for " + competition.toUpperCase());
        } else {
            matchUpdateService.updateMatches(); // triggers all leagues
            return ResponseEntity.ok("✅ Match update triggered manually for all leagues");
        }
    }

    @PostMapping("/matches/update-gameweek")
    public ResponseEntity<String> updateSpecificGameweek(
            @RequestParam(name = "competition", required = true) String competition,
            @RequestParam(name = "weekNumber", required = true) int weekNumber) {

        System.out.println("DEBUG: Controller method called successfully!");
        System.out.println("DEBUG: competition = " + competition);
        System.out.println("DEBUG: weekNumber = " + weekNumber);

        try {
            // Use the service mapping to get the LeagueTheme
            LeagueTheme league = matchUpdateService.mapToLeagueTheme(competition);

            if (league == null) {
                System.out.println("Invalid competition: " + competition);
                return ResponseEntity.badRequest().body("Invalid competition: " + competition);
            }

            System.out.println("Mapped to league: " + league);

            // Pass the original competition string
            matchUpdateService.updateMatchesForGameweek(competition, weekNumber);

            return ResponseEntity.ok("Match update triggered for " + league.name() + " week " + weekNumber);

        } catch (Exception e) {
            System.out.println("Exception in controller: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating matches for " + competition + " week " + weekNumber + ": " + e.getMessage());
        }
    }

}
