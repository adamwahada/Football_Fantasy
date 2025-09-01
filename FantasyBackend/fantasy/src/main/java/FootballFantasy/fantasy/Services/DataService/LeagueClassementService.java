package FootballFantasy.fantasy.Services.DataService;

import FootballFantasy.fantasy.Entities.GameweekEntity.*;
import FootballFantasy.fantasy.Repositories.GameweekRepository.MatchRepository;
import FootballFantasy.fantasy.Repositories.GameweekRepository.GameWeekRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class LeagueClassementService {

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private GameWeekRepository gameWeekRepository;

    @Autowired
    private MatchUpdateService matchUpdateService;

    // Get league table for a given competition up to a given gameweek
    public List<TeamStanding> getClassement(String competition, int gameweekNumber) {
        // 1️⃣ Find all GameWeeks up to the requested week
        List<GameWeek> pastWeeks = gameWeekRepository
                .findByCompetitionAndWeekNumberLessThanEqual(
                        LeagueTheme.valueOf(competition),
                        gameweekNumber
                );

        if (pastWeeks.isEmpty()) return Collections.emptyList();

        // 2️⃣ Aggregate matches from all these weeks
        List<Match> matches = new ArrayList<>();
        for (GameWeek gw : pastWeeks) {
            matches.addAll(matchRepository.findByGameweeksId(gw.getId()));
        }

        // 3️⃣ Aggregate standings
        Map<String, TeamStanding> standingsMap = new HashMap<>();

        for (Match m : matches) {
            if (m.getHomeScore() == null || m.getAwayScore() == null) continue; // skip unfinished

            String homeTeam = matchUpdateService.normalizeTeamName(m.getHomeTeam());
            String awayTeam = matchUpdateService.normalizeTeamName(m.getAwayTeam());

            standingsMap.putIfAbsent(homeTeam, new TeamStanding(homeTeam));
            standingsMap.putIfAbsent(awayTeam, new TeamStanding(awayTeam));

            standingsMap.get(homeTeam).addMatch(m.getHomeScore(), m.getAwayScore());
            standingsMap.get(awayTeam).addMatch(m.getAwayScore(), m.getHomeScore());
        }

        return standingsMap.values().stream()
                .sorted(
                        Comparator.comparing(TeamStanding::getPoints).reversed()
                                .thenComparing(Comparator.comparing(TeamStanding::getGoalDifference).reversed())
                                .thenComparing(Comparator.comparing(TeamStanding::getGoalsFor).reversed())
                )
                .collect(Collectors.toList());
    }

    // DTO for standings
    public static class TeamStanding {
        private String teamName;
        private int played = 0;
        private int won = 0;
        private int draw = 0;
        private int lost = 0;
        private int goalsFor = 0;
        private int goalsAgainst = 0;

        public TeamStanding(String teamName) { this.teamName = teamName; }

        public void addMatch(int scored, int conceded) {
            played++;
            goalsFor += scored;
            goalsAgainst += conceded;
            if (scored > conceded) won++;
            else if (scored == conceded) draw++;
            else lost++;
        }

        public int getPoints() { return won * 3 + draw; }
        public int getGoalDifference() { return goalsFor - goalsAgainst; }

        // Getters
        public String getTeamName() { return teamName; }
        public int getPlayed() { return played; }
        public int getWon() { return won; }
        public int getDraw() { return draw; }
        public int getLost() { return lost; }
        public int getGoalsFor() { return goalsFor; }
        public int getGoalsAgainst() { return goalsAgainst; }
    }
}
