package FootballFantasy.fantasy.Controller.DataController;


import FootballFantasy.fantasy.Services.DataService.MatchSeederService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/seed")
public class SeedController {

    @Autowired
    private MatchSeederService matchSeederService;

    @PostMapping("/gameweek/{weekNumber}")
    public ResponseEntity<String> seedGameWeek(@PathVariable int weekNumber) {
        try {
            matchSeederService.seedGameWeekByNumber(weekNumber);
            return ResponseEntity.ok("GameWeek " + weekNumber + " matches seeded successfully.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
}
