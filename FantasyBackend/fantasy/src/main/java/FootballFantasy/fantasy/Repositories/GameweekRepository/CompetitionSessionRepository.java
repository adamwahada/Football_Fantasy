package FootballFantasy.fantasy.Repositories.GameweekRepository;

import FootballFantasy.fantasy.Entities.GameweekEntity.CompetitionSession;
import FootballFantasy.fantasy.Entities.GameweekEntity.LeagueTheme;
import FootballFantasy.fantasy.Entities.GameweekEntity.SessionType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CompetitionSessionRepository extends JpaRepository<CompetitionSession, Long> {


    List<CompetitionSession> findByGameweekId(Long gameweekId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM CompetitionSession s WHERE s.gameweek.id = :gameweekId AND s.competition = :competition AND s.sessionType = :sessionType AND s.buyInAmount = :buyInAmount AND s.status = 'OPEN'")
    Optional<CompetitionSession> findAvailableSessionWithLock(@Param("gameweekId") Long gameweekId,
                                                              @Param("competition") LeagueTheme competition,
                                                              @Param("sessionType") SessionType sessionType,
                                                              @Param("buyInAmount") BigDecimal buyInAmount);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM CompetitionSession s WHERE s.accessKey = :accessKey AND s.competition = :competition AND s.status = 'OPEN'")
    Optional<CompetitionSession> findPrivateSessionByAccessKeyWithLock(@Param("accessKey") String accessKey,
                                                                       @Param("competition") LeagueTheme competition);

}
