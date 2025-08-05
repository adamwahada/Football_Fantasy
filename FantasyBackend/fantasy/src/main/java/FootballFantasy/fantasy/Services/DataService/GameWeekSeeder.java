package FootballFantasy.fantasy.Services.DataService;

import FootballFantasy.fantasy.Entities.GameweekEntity.GameWeek;
import FootballFantasy.fantasy.Entities.GameweekEntity.GameweekStatus;
import FootballFantasy.fantasy.Entities.GameweekEntity.LeagueTheme;
import FootballFantasy.fantasy.Entities.GameweekEntity.Match;
import FootballFantasy.fantasy.Repositories.GameweekRepository.GameWeekRepository;
import FootballFantasy.fantasy.Repositories.GameweekRepository.MatchRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

@Component
@RequiredArgsConstructor
public class GameWeekSeeder implements CommandLineRunner {

    private final GameWeekRepository gameWeekRepository;
    private final MatchRepository matchRepository;

    private static final List<LeagueTheme> TARGET_COMPETITIONS = List.of(
            LeagueTheme.PREMIER_LEAGUE,
            LeagueTheme.BUNDESLIGA,
            LeagueTheme.LIGUE_ONE,
            LeagueTheme.SERIE_A,
            LeagueTheme.LA_LIGA
    );

    @Override
    @Transactional
    public void run(String... args) {
        if (gameWeekRepository.count() > 0) return; // éviter les doublons

        int globalWeekNumber = 1;
        Random random = new Random();

        for (LeagueTheme competition : TARGET_COMPETITIONS) {
            for (int i = 1; i <= 38; i++) {
                GameWeek gw = new GameWeek();

                gw.setWeekNumber(i); // ✅ Use local week number per league
                gw.setStatus(GameweekStatus.UPCOMING);
                gw.setCompetition(competition);
                gw.setDescription("Semaine " + i + " - " + competition.name());

                // Simulate realistic dates
                LocalDateTime startDate = LocalDateTime.now().plusDays((i - 1) * 7L);
                LocalDateTime endDate = startDate.plusDays(3);
                LocalDateTime joinDeadline = startDate.minusHours(2);

                gw.setStartDate(startDate);
                gw.setEndDate(endDate);
                gw.setJoinDeadline(joinDeadline);

                // Optional: Only assign tiebreakers if matches are already available
                List<Match> allMatches = matchRepository.findAll();
                if (allMatches.size() >= 3) {
                    Collections.shuffle(allMatches);
                    List<Long> tiebreakerIds = allMatches.subList(0, 3).stream()
                            .map(Match::getId)
                            .toList();
                    gw.setTiebreakerMatchIdList(tiebreakerIds);
                }

                gameWeekRepository.save(gw);
            }
        }
        System.out.println("✅ GameWeeks seeded for top European leagues.");
    }
}
