package FootballFantasy.fantasy.Repositories.GameweekRepository;

import FootballFantasy.fantasy.Entities.GameweekEntity.ParticipationStatus;
import FootballFantasy.fantasy.Entities.GameweekEntity.SessionParticipation;
import FootballFantasy.fantasy.Entities.GameweekEntity.CompetitionSession;
import FootballFantasy.fantasy.Entities.GameweekEntity.SessionType;
import FootballFantasy.fantasy.Entities.UserEntity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SessionParticipationRepository extends JpaRepository<SessionParticipation, Long> {

    // Check if user already joined a specific session
    boolean existsByUserIdAndSessionId(Long userId, Long sessionId);

    // Get user's participation in a session
    Optional<SessionParticipation> findByUserAndSession(UserEntity user, CompetitionSession session);

    // Get all participations for a session (for ranking)
    List<SessionParticipation> findBySessionOrderByTotalCorrectPredictionsDesc(CompetitionSession session);

    // Get user's participations for a gameweek
    @Query("SELECT sp FROM SessionParticipation sp WHERE sp.user.id = :userId AND sp.session.gameweek.id = :gameweekId")
    List<SessionParticipation> findByUserIdAndGameweekId(@Param("userId") Long userId, @Param("gameweekId") Long gameweekId);

    // Get top performers in a session
    @Query("SELECT sp FROM SessionParticipation sp WHERE sp.session.id = :sessionId ORDER BY sp.totalCorrectPredictions DESC, sp.accuracyPercentage DESC")
    List<SessionParticipation> findTopPerformersInSession(@Param("sessionId") Long sessionId);

    // Get user's participations for a specific session type
    @Query("SELECT sp FROM SessionParticipation sp WHERE sp.user.id = :userId AND sp.session.sessionType = :sessionType")
    List<SessionParticipation> findByUserIdAndSessionType(@Param("userId") Long userId, @Param("sessionType") SessionType sessionType);

    // Get all active participations for a session
    @Query("SELECT sp FROM SessionParticipation sp WHERE sp.session.id = :sessionId AND sp.status = :status")
    List<SessionParticipation> findBySessionIdAndStatus(@Param("sessionId") Long sessionId, @Param("status") ParticipationStatus status);

    // Count participants in a session
    @Query("SELECT COUNT(sp) FROM SessionParticipation sp WHERE sp.session.id = :sessionId")
    Long countBySessionId(@Param("sessionId") Long sessionId);

    // Get user's participations with prizes won
    @Query("SELECT sp FROM SessionParticipation sp WHERE sp.user.id = :userId AND sp.prizeWon > 0")
    List<SessionParticipation> findByUserIdWithPrizes(@Param("userId") Long userId);

    // Get completed participations for a session
    @Query("SELECT sp FROM SessionParticipation sp WHERE sp.session.id = :sessionId AND sp.hasCompletedAllPredictions = true")
    List<SessionParticipation> findBySessionIdAndCompletedPredictions(@Param("sessionId") Long sessionId);

    // Get user's participation ranking in a session
    @Query("SELECT sp FROM SessionParticipation sp WHERE sp.session.id = :sessionId ORDER BY sp.accuracyPercentage DESC, sp.totalCorrectPredictions DESC")
    List<SessionParticipation> findBySessionIdOrderByPerformance(@Param("sessionId") Long sessionId);

}