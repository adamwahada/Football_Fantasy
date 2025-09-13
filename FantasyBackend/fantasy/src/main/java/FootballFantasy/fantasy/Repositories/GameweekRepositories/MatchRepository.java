package FootballFantasy.fantasy.Repositories.GameweekRepositories;

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
}

