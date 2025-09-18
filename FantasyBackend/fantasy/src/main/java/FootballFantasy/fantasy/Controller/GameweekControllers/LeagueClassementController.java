package FootballFantasy.fantasy.Controller.GameweekControllers;

import FootballFantasy.fantasy.Services.DataService.LeagueClassementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/league-classement")
public class LeagueClassementController {

    @Autowired
    private LeagueClassementService classementService;

    @GetMapping("/{competition}/{gameweek}")
    public List<LeagueClassementService.TeamStanding> getClassement(
            @PathVariable String competition,
            @PathVariable int gameweek
    ) {
        return classementService.getClassement(competition.toUpperCase(), gameweek);
    }
}