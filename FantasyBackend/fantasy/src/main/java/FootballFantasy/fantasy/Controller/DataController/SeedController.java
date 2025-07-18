    package FootballFantasy.fantasy.Controller.DataController;

    import FootballFantasy.fantasy.Entities.GameweekEntity.LeagueTheme;
    import FootballFantasy.fantasy.Repositories.GameweekRepository.GameWeekRepository;
    import FootballFantasy.fantasy.Services.DataService.MatchSeederService;
    import FootballFantasy.fantasy.Services.GameweekService.GameWeekService;
    import FootballFantasy.fantasy.Services.GameweekService.MatchService;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.http.ResponseEntity;
    import org.springframework.web.bind.annotation.*;

    @RestController
    @RequestMapping("/api/seed")
    public class SeedController {

        @Autowired
        private MatchSeederService matchSeederService;

        @Autowired
        private MatchService matchService;

        @Autowired
        private GameWeekService gameWeekService;

        @PostMapping("/gameweek/{league}/{weekNumber}")
        public ResponseEntity<String> seedGameWeek(
                @PathVariable LeagueTheme league,
                @PathVariable int weekNumber) {
            try {
                matchSeederService.seedGameWeekByNumberAndLeague(weekNumber, league);
                return ResponseEntity.ok("GameWeek " + weekNumber + " matches seeded successfully for " + league);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body("Invalid league or week: " + e.getMessage());
            } catch (Exception e) {
                return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
            }
        }

        /*** Update results for completed matches: homeScore, awayScore */

        @PostMapping("/gameweek/{league}/{weekNumber}/results")
        public ResponseEntity<String> updateGameWeekResults(
                @PathVariable LeagueTheme league,
                @PathVariable int weekNumber) {
            try {
                // Use MatchSeederService instead of MatchService
                matchSeederService.updateGameWeekResults(weekNumber, league);
                return ResponseEntity.ok("Results updated for GW " + weekNumber + " in " + league);
            } catch (IllegalArgumentException | IllegalStateException e) {
                return ResponseEntity.badRequest().body(e.getMessage());
            } catch (Exception e) {
                return ResponseEntity.internalServerError().body(e.getMessage());
            }
        }

        // Add this test endpoint
        @PostMapping("/test-gameweek-status/{gameweekId}")
        public ResponseEntity<String> testGameWeekStatus(@PathVariable Long gameweekId) {
            try {
                System.out.println("🧪 MANUAL TEST: Checking gameweek " + gameweekId + " status");
                boolean updated = gameWeekService.updateStatusIfComplete(gameweekId);

                if (updated) {
                    return ResponseEntity.ok("✅ GameWeek " + gameweekId + " status updated to FINISHED");
                } else {
                    return ResponseEntity.ok("⏳ GameWeek " + gameweekId + " is not complete yet or was already finished");
                }
            } catch (Exception e) {
                return ResponseEntity.internalServerError().body("❌ Error: " + e.getMessage());
            }
        }


    }
