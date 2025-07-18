package FootballFantasy.fantasy.Repositories.GameweekRepository;

import FootballFantasy.fantasy.Entities.GameweekEntity.CompetitionSession;
import FootballFantasy.fantasy.Entities.GameweekEntity.LeagueTheme;
import FootballFantasy.fantasy.Entities.GameweekEntity.SessionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;

public interface CompetitionSessionRepository extends JpaRepository<CompetitionSession, Long> {

    @Query("SELECT cs FROM CompetitionSession cs WHERE " +
            "cs.gameweek.id = :gameweekId AND " +
            "cs.competition = :competition AND " +
            "cs.sessionType = :sessionType AND " +
            "cs.buyInAmount = :buyInAmount " +
            "ORDER BY cs.createdAt ASC")
    List<CompetitionSession> findByGameweekAndCompetitionAndTypeAndBuyIn(
            @Param("gameweekId") Long gameweekId,
            @Param("competition") LeagueTheme competition,
            @Param("sessionType") SessionType sessionType,
            @Param("buyInAmount") BigDecimal buyInAmount
    );

    @Query("SELECT cs FROM CompetitionSession cs WHERE " +
            "cs.gameweek.id = :gameweekId AND " +
            "cs.competition = :competition AND " +
            "cs.sessionType = :sessionType AND " +
            "cs.buyInAmount = :buyInAmount AND " +
            "cs.status = 'OPEN' AND " +
            "cs.currentParticipants < cs.maxParticipants AND " +
            "cs.joinDeadline > CURRENT_TIMESTAMP AND " +
            "cs.accessKey IS NULL " +
            "ORDER BY cs.createdAt ASC")
    CompetitionSession findAvailableSession(
            @Param("gameweekId") Long gameweekId,
            @Param("competition") LeagueTheme competition,
            @Param("sessionType") SessionType sessionType,
            @Param("buyInAmount") BigDecimal buyInAmount
    );

    @Query("SELECT cs FROM CompetitionSession cs WHERE " +
            "cs.accessKey = :accessKey AND " +
            "cs.competition = :competition AND " +
            "cs.status = 'OPEN' AND " +
            "cs.currentParticipants < cs.maxParticipants AND " +
            "cs.joinDeadline > CURRENT_TIMESTAMP")
    CompetitionSession findPrivateSessionByAccessKey(
            @Param("accessKey") String accessKey,
            @Param("competition") LeagueTheme competition
    );

    List<CompetitionSession> findByGameweekId(Long gameweekId);
    List<CompetitionSession> findAllByGameweekId(Long gameweekId);
}
