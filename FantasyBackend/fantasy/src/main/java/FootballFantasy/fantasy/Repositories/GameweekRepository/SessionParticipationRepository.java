package FootballFantasy.fantasy.Repositories.GameweekRepository;

import FootballFantasy.fantasy.Entities.GameweekEntity.SessionParticipation;
import FootballFantasy.fantasy.Entities.GameweekEntity.CompetitionSession;
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
}