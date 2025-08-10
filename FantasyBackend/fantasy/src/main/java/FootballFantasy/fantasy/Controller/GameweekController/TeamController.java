package FootballFantasy.fantasy.Controller.GameweekController;

import FootballFantasy.fantasy.Services.DataService.TeamIconService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    @Autowired
    private TeamIconService teamIconService;

    @GetMapping("/icons")
    public Map<String, String> getAllTeamIcons() {
        return teamIconService.getAllTeamIcons();
    }

    @GetMapping("/{teamName}/icon")
    public String getTeamIcon(@PathVariable String teamName) {
        return teamIconService.getTeamIcon(teamName);
    }

    @GetMapping("/leagues/icons")
    public Map<String, String> getAllLeagueIcons() {
        return teamIconService.getAllLeagues();
    }

    @GetMapping("/leagues/{leagueName}/icon")
    public String getLeagueIcon(@PathVariable String leagueName) {
        return teamIconService.getLeagueIcon(leagueName);
    }
}
