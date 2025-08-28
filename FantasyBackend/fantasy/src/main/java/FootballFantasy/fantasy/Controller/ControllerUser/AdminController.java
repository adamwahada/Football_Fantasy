package FootballFantasy.fantasy.Controller.ControllerUser;

import FootballFantasy.fantasy.Services.UserService.UserService;
import FootballFantasy.fantasy.Services.DataService.MatchUpdateService;
import org.springframework.beans.factory.annotation.Autowired;
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
}
