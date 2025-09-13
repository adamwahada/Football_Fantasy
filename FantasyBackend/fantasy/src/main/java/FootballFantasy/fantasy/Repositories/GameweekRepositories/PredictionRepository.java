package FootballFantasy.fantasy.Repositories.GameweekRepositories;

import FootballFantasy.fantasy.Entities.GameweekEntities.Prediction;
import FootballFantasy.fantasy.Entities.GameweekEntities.PredictionResult;
import FootballFantasy.fantasy.Entities.GameweekEntities.SessionParticipation;
import FootballFantasy.fantasy.Entities.GameweekEntities.SessionType;
import FootballFantasy.fantasy.Entities.GameweekEntities.LeagueTheme;
import FootballFantasy.fantasy.Entities.UserEntities.UserEntity;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PredictionRepository extends JpaRepository<Prediction, Long> {

    // 🔍 Find predictions by participation
    List<Prediction> findByParticipation(SessionParticipation participation);

    // 🔍 Find predictions by user and session (before participation is created)
    @Query("SELECT p FROM Prediction p WHERE p.user = :user AND p.participation.session.id = :sessionId")
    List<Prediction> findByUserAndSessionId(@Param("user") UserEntity user, @Param("sessionId") Long sessionId);

    // 🔍 Find predictions by participation and match
    Optional<Prediction> findByParticipationAndMatchId(SessionParticipation participation, Long matchId);

    // 🎯 Find tiebreaker predictions only
    List<Prediction> findByParticipationAndIsTiebreakerTrue(SessionParticipation participation);

    // 🏆 Find completed predictions (for accuracy calculation)
    @Query("SELECT p FROM Prediction p " +
            "JOIN p.match m " +
            "WHERE p.participation.id = :participationId " +
            "AND m.status = 'COMPLETED'")
    List<Prediction> findCompletedPredictionsByParticipation(@Param("participationId") Long participationId);

    // 📊 Count correct predictions for a participation
    @Query("SELECT COUNT(p) FROM Prediction p WHERE p.participation = :participation AND p.isCorrect = true")
    Long countCorrectPredictionsByParticipation(@Param("participation") SessionParticipation participation);

    // 📊 Count total predictions for a participation
    @Query("SELECT COUNT(p) FROM Prediction p WHERE p.participation = :participation AND p.match.status = 'FINISHED'")
    Long countTotalPredictionsByParticipation(@Param("participation") SessionParticipation participation);

    // 🎲 Find all predictions for a specific match
    List<Prediction> findByMatchId(Long matchId);

    // 🔒 Check if user already predicted for a match in session
    boolean existsByParticipationAndMatchId(SessionParticipation participation, Long matchId);

    // 🔒 Check if user has predictions for a gameweek/session combination (before joining)
    @Query("SELECT COUNT(p) > 0 FROM Prediction p WHERE p.user.id = :userId AND p.participation.session.gameweek.id = :gameweekId AND p.participation.session.sessionType = :sessionType AND p.participation.session.buyInAmount = :buyInAmount AND p.participation.session.competition = :competition")
    boolean existsByUserAndGameweekSession(@Param("userId") Long userId,
                                           @Param("gameweekId") Long gameweekId,
                                           @Param("sessionType") SessionType sessionType,
                                           @Param("buyInAmount") BigDecimal buyInAmount,
                                           @Param("competition") LeagueTheme competition);

    // 📈 Get predictions by result type
    List<Prediction> findByParticipationAndPredictedResult(SessionParticipation participation, PredictionResult result);

    // 🎯 Get tiebreaker predictions with valid scores
    @Query("SELECT p FROM Prediction p WHERE p.participation = :participation AND p.isTiebreaker = true AND p.predictedHomeScore IS NOT NULL AND p.predictedAwayScore IS NOT NULL")
    List<Prediction> findValidTiebreakerPredictions(@Param("participation") SessionParticipation participation);

    // 🏅 Get best tiebreaker performance (lowest average distance)
    @Query("SELECT p.participation, AVG(p.scoreDistance) as avgDistance FROM Prediction p WHERE p.isTiebreaker = true AND p.scoreDistance IS NOT NULL GROUP BY p.participation ORDER BY avgDistance ASC")
    List<Object[]> findBestTiebreakerPerformances();

    // 🗑️ Delete predictions by participation (for cleanup)
    void deleteByParticipation(SessionParticipation participation);

    // 🎯 Get tiebreaker score for ranking
    @Query("SELECT COALESCE(AVG(p.scoreDistance), 999999.0) FROM Prediction p WHERE p.participation.id = :participationId AND p.isTiebreaker = true AND p.scoreDistance IS NOT NULL")
    Double getTiebreakerScore(@Param("participationId") Long participationId);
}