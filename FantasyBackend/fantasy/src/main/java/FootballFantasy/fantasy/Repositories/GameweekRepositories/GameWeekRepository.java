package FootballFantasy.fantasy.Repositories.GameweekRepositories;

import FootballFantasy.fantasy.Entities.GameweekEntities.GameWeek;
import FootballFantasy.fantasy.Entities.GameweekEntities.LeagueTheme;
import org.springframework.data.jpa.repository.EntityGraph;
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

    @EntityGraph(attributePaths = "matches")
    Optional<GameWeek> findByCompetitionAndWeekNumber(LeagueTheme competition, int weekNumber);

    @EntityGraph(attributePaths = "matches")
    List<GameWeek> findByCompetitionAndJoinDeadlineAfter(LeagueTheme competition, LocalDateTime now);

    List<GameWeek> findByCompetitionAndWeekNumberLessThanEqual(LeagueTheme competition, int weekNumber);
    List<GameWeek> findByCompetitionOrderByWeekNumber(LeagueTheme competition);

}
