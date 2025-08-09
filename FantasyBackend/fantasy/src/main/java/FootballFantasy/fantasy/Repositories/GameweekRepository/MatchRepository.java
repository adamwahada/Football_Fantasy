package FootballFantasy.fantasy.Repositories.GameweekRepository;

import FootballFantasy.fantasy.Entities.GameweekEntity.LeagueTheme;
import FootballFantasy.fantasy.Entities.GameweekEntity.Match;
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

}