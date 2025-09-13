package FootballFantasy.fantasy.Repositories.GameweekRepositories;

import FootballFantasy.fantasy.Entities.GameweekEntities.CompetitionSession;
import FootballFantasy.fantasy.Entities.GameweekEntities.CompetitionSessionStatus;
import FootballFantasy.fantasy.Entities.GameweekEntities.LeagueTheme;
import FootballFantasy.fantasy.Entities.GameweekEntities.SessionType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CompetitionSessionRepository extends JpaRepository<CompetitionSession, Long> {

    List<CompetitionSession> findByGameweekId(Long gameweekId);

    // 🔍 Find available public session (not locked - for preview only)
    @Query("SELECT s FROM CompetitionSession s WHERE s.gameweek.id = :gameweekId AND s.competition = :competition AND s.sessionType = :sessionType AND s.buyInAmount = :buyInAmount AND s.status = 'OPEN' AND s.accessKey IS NULL ORDER BY s.createdAt ASC")
    Optional<CompetitionSession> findAvailablePublicSession(@Param("gameweekId") Long gameweekId,
                                                            @Param("competition") LeagueTheme competition,
                                                            @Param("sessionType") SessionType sessionType,
                                                            @Param("buyInAmount") BigDecimal buyInAmount);

    // 🔒 Find and lock available public session (for actual joining)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM CompetitionSession s WHERE s.gameweek.id = :gameweekId AND s.competition = :competition AND s.sessionType = :sessionType AND s.buyInAmount = :buyInAmount AND s.status = 'OPEN' AND s.accessKey IS NULL AND s.currentParticipants < s.maxParticipants ORDER BY s.createdAt ASC")
    Optional<CompetitionSession> findAvailableSessionWithLock(@Param("gameweekId") Long gameweekId,
                                                              @Param("competition") LeagueTheme competition,
                                                              @Param("sessionType") SessionType sessionType,
                                                              @Param("buyInAmount") BigDecimal buyInAmount);

    // 🔍 UPDATED: Find private session by access key for specific gameweek (not locked - for preview)
    @Query("SELECT s FROM CompetitionSession s WHERE s.accessKey = :accessKey AND s.competition = :competition AND s.gameweek.id = :gameweekId AND s.status = 'OPEN'")
    Optional<CompetitionSession> findPrivateSessionByAccessKeyAndGameweek(@Param("accessKey") String accessKey,
                                                                          @Param("competition") LeagueTheme competition,
                                                                          @Param("gameweekId") Long gameweekId);

    // 🔒 UPDATED: Find and lock private session by access key for specific gameweek (for actual joining)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM CompetitionSession s WHERE s.accessKey = :accessKey AND s.competition = :competition AND s.gameweek.id = :gameweekId AND s.status = 'OPEN' AND s.currentParticipants < s.maxParticipants")
    Optional<CompetitionSession> findPrivateSessionByAccessKeyWithLock(@Param("accessKey") String accessKey,
                                                                       @Param("competition") LeagueTheme competition,
                                                                       @Param("gameweekId") Long gameweekId);

    // 🆕 NEW: Check if private session exists for access key and gameweek (regardless of status/availability)
    @Query("SELECT s FROM CompetitionSession s WHERE s.accessKey = :accessKey AND s.competition = :competition AND s.gameweek.id = :gameweekId")
    Optional<CompetitionSession> findPrivateSessionByAccessKeyAndGameweekAnyStatus(@Param("accessKey") String accessKey,
                                                                                   @Param("competition") LeagueTheme competition,
                                                                                   @Param("gameweekId") Long gameweekId);

    // 🆕 NEW: Check if access key exists for any gameweek (to differentiate between "doesn't exist" vs "wrong gameweek")
    @Query("SELECT s FROM CompetitionSession s WHERE s.accessKey = :accessKey AND s.competition = :competition")
    Optional<CompetitionSession> findPrivateSessionByAccessKeyAnyGameweek(@Param("accessKey") String accessKey,
                                                                          @Param("competition") LeagueTheme competition);

    // 🔍 Check if user can join this specific session type/amount for gameweek
    @Query("SELECT COUNT(sp) > 0 FROM SessionParticipation sp WHERE sp.user.id = :userId AND sp.session.gameweek.id = :gameweekId AND sp.session.sessionType = :sessionType AND sp.session.buyInAmount = :buyInAmount AND sp.session.competition = :competition")
    boolean existsUserParticipationForGameweekSession(@Param("userId") Long userId,
                                                      @Param("gameweekId") Long gameweekId,
                                                      @Param("sessionType") SessionType sessionType,
                                                      @Param("buyInAmount") BigDecimal buyInAmount,
                                                      @Param("competition") LeagueTheme competition);

    List<CompetitionSession> findByStatusAndSessionTypeAndMaxParticipantsAndJoinDeadlineBefore(
            CompetitionSessionStatus status,
            SessionType type,
            int maxParticipants,
            LocalDateTime deadline
    );

    @Query("SELECT cs FROM CompetitionSession cs WHERE cs.status = :status AND cs.joinDeadline < :deadline")
    List<CompetitionSession> findByStatusAndJoinDeadlineBefore(
            @Param("status") CompetitionSessionStatus status,
            @Param("deadline") LocalDateTime deadline
    );
}