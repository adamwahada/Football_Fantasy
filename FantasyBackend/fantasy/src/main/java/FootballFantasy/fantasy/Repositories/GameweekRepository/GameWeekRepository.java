package FootballFantasy.fantasy.Repositories.GameweekRepository;

import FootballFantasy.fantasy.Entities.GameweekEntity.GameWeek;
import FootballFantasy.fantasy.Entities.GameweekEntity.LeagueTheme;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GameWeekRepository extends JpaRepository<GameWeek, Long> {
    Optional<GameWeek> findByWeekNumber(int weekNumber);
    List<GameWeek> findByCompetition(LeagueTheme Competition);


}