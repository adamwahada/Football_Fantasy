package FootballFantasy.fantasy.Controller.UserControllers;

import FootballFantasy.fantasy.Entities.AdminEntities.BanCause;
import FootballFantasy.fantasy.Entities.GameweekEntities.LeagueTheme;
import FootballFantasy.fantasy.Entities.UserEntities.UserEntity;
import FootballFantasy.fantasy.Services.GameweekService.CompetitionSessionService;
import FootballFantasy.fantasy.Services.UserService.UserService;
import FootballFantasy.fantasy.Services.DataService.MatchUpdateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private MatchUpdateService matchUpdateService;
    @Autowired
    private CompetitionSessionService competitionSessionService;

//    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/users")
    public ResponseEntity<List<UserEntity>> getAllUsers() {
        List<UserEntity> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    // ✅ Credit user balance
//    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/users/{userId}/credit/{adminId}")
    public ResponseEntity<String> credit(
            @PathVariable Long userId,
            @RequestParam BigDecimal amount,
            @PathVariable Long adminId
    ) {
        userService.creditBalance(userId, amount, adminId);
        return ResponseEntity.ok("Balance updated");
    }


    // ✅ Debit user balance
//    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/users/{userId}/debit/{adminId}")
    public ResponseEntity<String> debit(@PathVariable Long userId,
                                        @RequestParam BigDecimal amount,
    @PathVariable Long adminId) {
        userService.debitBalance(userId, amount,adminId);
        return ResponseEntity.ok("Balance debited");
    }

    // ✅ Manual trigger for match updates
//    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/matches/update-now")
    public ResponseEntity<String> triggerMatchUpdate(
            @RequestParam(required = false) String competition) {

        try {
            matchUpdateService.updateTodaysMatches();

            return ResponseEntity.ok("✅ Today's matches updated successfully across all competitions");

        } catch (Exception e) {
            System.out.println("❌ Error updating today's matches: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error updating today's matches: " + e.getMessage());
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

    // =================== USER BAN MANAGEMENT ===================

    @PostMapping("/users/{userId}/ban-temporary/{adminId}")
    public ResponseEntity<String> banUserTemporarily(
            @PathVariable Long userId,
            @RequestParam int days,@PathVariable Long adminId,@RequestBody BanCause reason) {

        userService.banUserTemporarily(userId, days,adminId,reason);
        return ResponseEntity.ok("User temporarily banned for " + days + " day(s)");
    }

    @PostMapping("/users/{userId}/ban-permanent/{adminId}")
    public ResponseEntity<String> banUserPermanently(@PathVariable Long userId,@PathVariable Long adminId,@RequestBody BanCause reason) {

        userService.banUserPermanently(userId,adminId,reason);
        return ResponseEntity.ok("User permanently banned");
    }

    @PostMapping("/users/{userId}/unban/{adminId}")
    public ResponseEntity<String> unbanUser(@PathVariable Long userId,@PathVariable Long adminId) {

        userService.unbanUser(userId,adminId);
        return ResponseEntity.ok("User unbanned successfully");
    }

    @GetMapping("/users/{userId}/ban-status")
    public ResponseEntity<String> getUserBanStatus(@PathVariable Long userId) {

        String status = userService.getUserBanStatus(userId);
        return ResponseEntity.ok("User status: " + status);
    }
    // =================== SESSION MANAGEMENT ===================

    // 1️⃣ Cancel session with automatic refunds
        @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/sessions/{sessionId}/cancel-with-refunds")
    public ResponseEntity<String> cancelSessionWithRefunds(@PathVariable Long sessionId) {
        try {
            competitionSessionService.cancelSessionWithAutomaticRefund(sessionId);
            return ResponseEntity.ok("Session " + sessionId + " cancelled and all participants refunded.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error cancelling session with refunds: " + e.getMessage());
        }
    }

    // 2️⃣ Cancel session manually (no refunds)
    @PostMapping("/sessions/{sessionId}/cancel-manually")
    public ResponseEntity<String> cancelSessionManually(@PathVariable Long sessionId) {
        try {
            competitionSessionService.cancelSessionManually(sessionId);
            return ResponseEntity.ok("Session " + sessionId + " cancelled manually (no automatic refunds).");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error cancelling session manually: " + e.getMessage());
        }
    }
    // 3️⃣ Refund a previously cancelled session

    @PostMapping("/sessions/{sessionId}/refund-cancelled")
    public ResponseEntity<String> refundCancelledSession(@PathVariable Long sessionId) {
        try {
            competitionSessionService.refundCancelledSession(sessionId);
            return ResponseEntity.ok("Refunds processed for cancelled session " + sessionId);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing refunds: " + e.getMessage());
        }
    }


}
