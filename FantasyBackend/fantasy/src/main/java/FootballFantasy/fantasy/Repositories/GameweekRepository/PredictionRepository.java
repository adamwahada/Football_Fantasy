package FootballFantasy.fantasy.Repositories.GameweekRepository;

import FootballFantasy.fantasy.Entities.GameweekEntity.Prediction;
import FootballFantasy.fantasy.Entities.GameweekEntity.Match;
import FootballFantasy.fantasy.Entities.GameweekEntity.SessionParticipation;
import FootballFantasy.fantasy.Entities.UserEntity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PredictionRepository extends JpaRepository<Prediction, Long> {

    // Get user's prediction for a specific match in a session
    Optional<Prediction> findByUserAndMatchAndParticipation(UserEntity user, Match match, SessionParticipation participation);

    // Get all predictions for a participation
    List<Prediction> findByParticipation(SessionParticipation participation);

    // Get all predictions for a match
    List<Prediction> findByMatch(Match match);

    // Check if user has predicted for a match in a session
    boolean existsByUserAndMatchAndParticipation(UserEntity user, Match match, SessionParticipation participation);

    // Get user's predictions for a gameweek session
    @Query("SELECT p FROM Prediction p WHERE p.user.id = :userId AND p.participation.session.gameweek.id = :gameweekId AND p.participation.session.id = :sessionId")
    List<Prediction> findByUserAndGameweekAndSession(@Param("userId") Long userId, @Param("gameweekId") Long gameweekId, @Param("sessionId") Long sessionId);

    // Count correct predictions for a participation
    @Query("SELECT COUNT(p) FROM Prediction p WHERE p.participation.id = :participationId AND p.isCorrect = true")
    Long countCorrectPredictionsByParticipation(@Param("participationId") Long participationId);
}