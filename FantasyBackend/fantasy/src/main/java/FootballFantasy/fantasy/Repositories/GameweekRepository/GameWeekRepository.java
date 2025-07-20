package FootballFantasy.fantasy.Repositories.GameweekRepository;

import FootballFantasy.fantasy.Entities.GameweekEntity.GameWeek;
import FootballFantasy.fantasy.Entities.GameweekEntity.LeagueTheme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface GameWeekRepository extends JpaRepository<GameWeek, Long> {
    Optional<GameWeek> findByWeekNumber(int weekNumber);
    List<GameWeek> findByCompetition(LeagueTheme Competition);
    Optional<GameWeek> findByWeekNumberAndCompetition(int weekNumber, LeagueTheme competition);
    @Query("""
    SELECT gw FROM GameWeek gw
    LEFT JOIN FETCH gw.matches m
    WHERE gw.id = :id
  """)
    GameWeek findWithMatchesById(@Param("id") Long id);
    Optional<GameWeek> findByCompetitionAndWeekNumber(LeagueTheme competition, int weekNumber);

    List<GameWeek> findByCompetitionAndJoinDeadlineAfter(LeagueTheme competition, LocalDateTime now);

}