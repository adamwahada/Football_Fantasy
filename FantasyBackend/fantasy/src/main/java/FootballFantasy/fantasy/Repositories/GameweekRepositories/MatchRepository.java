package FootballFantasy.fantasy.Repositories.GameweekRepositories;

import FootballFantasy.fantasy.Entities.GameweekEntities.GameWeek;
import FootballFantasy.fantasy.Entities.GameweekEntities.LeagueTheme;
import FootballFantasy.fantasy.Entities.GameweekEntities.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, Long> {
    List<Match> findByGameweeksId(Long gameWeekId);
    Match findByHomeTeamAndAwayTeamAndMatchDate(String homeTeam, String awayTeam, LocalDateTime matchDate);

    @Query("SELECT m FROM Match m WHERE m.homeTeam = :homeTeam " +
            "AND m.awayTeam = :awayTeam " +
            "AND FUNCTION('DATE', m.matchDate) = FUNCTION('DATE', :matchDate)")
    Optional<Match> findDuplicateMatch(@Param("homeTeam") String homeTeam,
                                       @Param("awayTeam") String awayTeam,
                                       @Param("matchDate") LocalDateTime matchDate);

    List<Match> findByGameweeksIdAndActiveTrue(Long gameweekId);

    // ✅ New method to fetch match with gameweeks
    @Query("SELECT m FROM Match m LEFT JOIN FETCH m.gameweeks " +
            "WHERE m.homeTeam = :home AND m.awayTeam = :away AND m.matchDate BETWEEN :from AND :to")
    Match findWithGameweeks(
            @Param("home") String home,
            @Param("away") String away,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    // ✅ Missing method needed by MatchUpdateService - find matches that belong to a specific gameweek
    @Query("SELECT m FROM Match m WHERE :gameweek MEMBER OF m.gameweeks")
    List<Match> findByGameweeksContaining(@Param("gameweek") GameWeek gameweek);

    // ✅ Additional useful query methods for match management

    // Find all active matches for a specific gameweek
    @Query("SELECT m FROM Match m WHERE :gameweek MEMBER OF m.gameweeks AND m.active = true")
    List<Match> findActiveByGameweeksContaining(@Param("gameweek") GameWeek gameweek);

    // Find matches by status
    @Query("SELECT m FROM Match m WHERE m.status = :status")
    List<Match> findByStatus(@Param("status") String status);

    // Find matches within a date range
    @Query("SELECT m FROM Match m WHERE m.matchDate BETWEEN :startDate AND :endDate ORDER BY m.matchDate")
    List<Match> findByMatchDateBetween(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

    // Find finished matches
    List<Match> findByFinishedTrue();

    // Find matches that need prediction deadline update (matches happening soon)
    @Query("SELECT m FROM Match m WHERE m.matchDate > CURRENT_TIMESTAMP " +
            "AND m.matchDate <= :deadline AND m.active = true")
    List<Match> findMatchesApproachingDeadline(@Param("deadline") LocalDateTime deadline);

    // Find matches by team (either home or away)
    @Query("SELECT m FROM Match m WHERE m.homeTeam = :team OR m.awayTeam = :team ORDER BY m.matchDate DESC")
    List<Match> findByTeam(@Param("team") String team);

    // Find today's matches
    @Query("SELECT m FROM Match m WHERE FUNCTION('DATE', m.matchDate) = FUNCTION('DATE', CURRENT_TIMESTAMP) " +
            "AND m.active = true ORDER BY m.matchDate")
    List<Match> findTodaysMatches();

    // Count matches in a gameweek by status
    @Query("SELECT COUNT(m) FROM Match m WHERE :gameweek MEMBER OF m.gameweeks AND m.finished = :finished")
    long countByGameweekAndFinished(@Param("gameweek") GameWeek gameweek, @Param("finished") boolean finished);

    // ✅ ADD: Find active matches by competition and week
    @Query("SELECT m FROM Match m JOIN m.gameweeks gw WHERE gw.competition = :competition AND gw.weekNumber = :weekNumber AND m.active = true")
    List<Match> findActiveMatchesByCompetitionAndWeek(@Param("competition") LeagueTheme competition, @Param("weekNumber") int weekNumber);

    // ✅ ADD: Count active matches in a gameweek
    @Query("SELECT COUNT(m) FROM Match m JOIN m.gameweeks gw WHERE gw.id = :gameweekId AND m.active = true")
    long countActiveMatchesByGameweek(@Param("gameweekId") Long gameweekId);

}