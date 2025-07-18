package FootballFantasy.fantasy.Repositories.GameweekRepository;

import FootballFantasy.fantasy.Entities.GameweekEntity.*;
import FootballFantasy.fantasy.Entities.UserEntity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface SessionParticipationRepository extends JpaRepository<SessionParticipation, Long> {

    // Check if user already joined a specific session
    boolean existsByUserIdAndSessionId(Long userId, Long sessionId);

    // Get user's participation in a session (by user ID)
    Optional<SessionParticipation> findByUserIdAndSessionId(Long userId, Long sessionId);

    // Get user's participation in a session (by user entity)
    Optional<SessionParticipation> findByUserAndSession(UserEntity user, CompetitionSession session);

    // Get all participations for a user
    List<SessionParticipation> findByUserId(Long userId);

    // ===== GAMEWEEK RELATED QUERIES =====

    // Get user's participations for a gameweek
    @Query("SELECT sp FROM SessionParticipation sp WHERE sp.user.id = :userId AND sp.session.gameweek.id = :gameweekId")
    List<SessionParticipation> findByUserIdAndGameweekId(@Param("userId") Long userId, @Param("gameweekId") Long gameweekId);

    // Check if user already has participation for specific criteria (prevents duplicate entries)
    @Query("SELECT CASE WHEN COUNT(sp) > 0 THEN true ELSE false END FROM SessionParticipation sp " +
            "WHERE sp.user.id = :userId " +
            "AND sp.session.gameweek.weekNumber = :weekNumber " +
            "AND sp.session.competition = :competition " +
            "AND sp.session.sessionType = :sessionType " +
            "AND sp.session.buyInAmount = :buyInAmount")

    boolean existsByUserIdAndGameweekIdAndSessionTypeAndBuyInAmountAndSession_Competition(
            @Param("userId") Long userId,
            @Param("weekNumber") Long weekNumber,
            @Param("sessionType") SessionType sessionType,
            @Param("buyInAmount") BigDecimal buyInAmount,
            @Param("competition") LeagueTheme competition

    );

    // ===== SESSION RELATED QUERIES =====

    // Get all participations for a session (for ranking by accuracy)
    @Query("SELECT sp FROM SessionParticipation sp WHERE sp.session.id = :sessionId ORDER BY sp.accuracyPercentage DESC, sp.totalCorrectPredictions DESC")
    List<SessionParticipation> findBySessionIdOrderByAccuracyPercentageDesc(@Param("sessionId") Long sessionId);

    // Get all participations for a session (original method - ordered by correct predictions)
    List<SessionParticipation> findBySessionOrderByTotalCorrectPredictionsDesc(CompetitionSession session);

    // Get all active participations for a session
    @Query("SELECT sp FROM SessionParticipation sp WHERE sp.session.id = :sessionId AND sp.status = :status")
    List<SessionParticipation> findBySessionIdAndStatus(@Param("sessionId") Long sessionId, @Param("status") ParticipationStatus status);

    // Count participants in a session
    @Query("SELECT COUNT(sp) FROM SessionParticipation sp WHERE sp.session.id = :sessionId")
    Long countBySessionId(@Param("sessionId") Long sessionId);

    // Get completed participations for a session
    @Query("SELECT sp FROM SessionParticipation sp WHERE sp.session.id = :sessionId AND sp.hasCompletedAllPredictions = true")
    List<SessionParticipation> findBySessionIdAndCompletedPredictions(@Param("sessionId") Long sessionId);

    // Get user's participation ranking in a session
    @Query("SELECT sp FROM SessionParticipation sp WHERE sp.session.id = :sessionId ORDER BY sp.accuracyPercentage DESC, sp.totalCorrectPredictions DESC")
    List<SessionParticipation> findBySessionIdOrderByPerformance(@Param("sessionId") Long sessionId);

    // ===== USER STATUS AND ACTIVITY QUERIES =====

    // Get user's participations by status
    @Query("SELECT sp FROM SessionParticipation sp WHERE sp.user.id = :userId AND sp.status = :status")
    List<SessionParticipation> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") ParticipationStatus status);

    // Get user's participations for a specific session type
    @Query("SELECT sp FROM SessionParticipation sp WHERE sp.user.id = :userId AND sp.session.sessionType = :sessionType")
    List<SessionParticipation> findByUserIdAndSessionType(@Param("userId") Long userId, @Param("sessionType") SessionType sessionType);

    // ===== FINANCIAL QUERIES =====

    // Get user's participations with prizes won
    @Query("SELECT sp FROM SessionParticipation sp WHERE sp.user.id = :userId AND sp.prizeWon > 0")
    List<SessionParticipation> findByUserIdWithPrizes(@Param("userId") Long userId);

    // Sum total prize winnings for a user
    @Query("SELECT COALESCE(SUM(sp.prizeWon), 0) FROM SessionParticipation sp WHERE sp.user.id = :userId")
    BigDecimal sumPrizeWonByUserId(@Param("userId") Long userId);

    // Sum total amount paid by user
    @Query("SELECT COALESCE(SUM(sp.amountPaid), 0) FROM SessionParticipation sp WHERE sp.user.id = :userId")
    BigDecimal sumAmountPaidByUserId(@Param("userId") Long userId);

    // ===== PERFORMANCE QUERIES =====

    // Get top performers in a session
    @Query("SELECT sp FROM SessionParticipation sp WHERE sp.session.id = :sessionId ORDER BY sp.totalCorrectPredictions DESC, sp.accuracyPercentage DESC")
    List<SessionParticipation> findTopPerformersInSession(@Param("sessionId") Long sessionId);

    // Get user's best performances (top accuracy)
    @Query("SELECT sp FROM SessionParticipation sp WHERE sp.user.id = :userId ORDER BY sp.accuracyPercentage DESC")
    List<SessionParticipation> findByUserIdOrderByAccuracyDesc(@Param("userId") Long userId);

    // Get user's won sessions (rank = 1)
    @Query("SELECT sp FROM SessionParticipation sp WHERE sp.user.id = :userId AND sp.rank = 1")
    List<SessionParticipation> findWonSessionsByUserId(@Param("userId") Long userId);

    // ===== STATISTICS QUERIES =====

    // Count total sessions participated by user
    @Query("SELECT COUNT(sp) FROM SessionParticipation sp WHERE sp.user.id = :userId")
    Long countTotalSessionsByUserId(@Param("userId") Long userId);

    // Count won sessions by user
    @Query("SELECT COUNT(sp) FROM SessionParticipation sp WHERE sp.user.id = :userId AND sp.rank = 1")
    Long countWonSessionsByUserId(@Param("userId") Long userId);

    // Get average accuracy for user
    @Query("SELECT AVG(sp.accuracyPercentage) FROM SessionParticipation sp WHERE sp.user.id = :userId")
    Double getAverageAccuracyByUserId(@Param("userId") Long userId);

    // ===== ADMIN/REPORTING QUERIES =====

    // Get all participations for a gameweek
    @Query("SELECT sp FROM SessionParticipation sp WHERE sp.session.gameweek.id = :gameweekId")
    List<SessionParticipation> findByGameweekId(@Param("gameweekId") Long gameweekId);

    // Get participations by session type
    @Query("SELECT sp FROM SessionParticipation sp WHERE sp.session.sessionType = :sessionType")
    List<SessionParticipation> findBySessionType(@Param("sessionType") SessionType sessionType);

    // Get recent participations for user
    @Query("SELECT sp FROM SessionParticipation sp WHERE sp.user.id = :userId ORDER BY sp.joinedAt DESC")
    List<SessionParticipation> findByUserIdOrderByJoinedAtDesc(@Param("userId") Long userId);
}
