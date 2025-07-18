package FootballFantasy.fantasy.Repositories.GameweekRepository;

import FootballFantasy.fantasy.Entities.GameweekEntity.GameWeek;
import FootballFantasy.fantasy.Entities.GameweekEntity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, Long> {
    List<Match> findByGameweeksId(Long gameWeekId);
    Optional<Match> findByHomeTeamAndAwayTeamAndMatchDate(String homeTeam, String awayTeam, LocalDateTime matchDate);

}