package FootballFantasy.fantasy.Services.GameweekService;

import FootballFantasy.fantasy.Entities.GameweekEntity.*;
import FootballFantasy.fantasy.Entities.UserEntity.UserEntity;
import FootballFantasy.fantasy.Repositories.GameweekRepository.CompetitionSessionRepository;
import FootballFantasy.fantasy.Repositories.GameweekRepository.SessionParticipationRepository;
import FootballFantasy.fantasy.Repositories.UserRepository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SessionParticipationService {

    @Autowired
    private SessionParticipationRepository sessionParticipationRepository;

    @Autowired
    private CompetitionSessionRepository competitionSessionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompetitionSessionService competitionSessionService;

    /**
     * Complete flow: Find/Create session and join it
     */
    @Transactional
    public SessionParticipation joinCompetition(Long gameweekId, SessionType sessionType,
                                                BigDecimal buyInAmount, boolean isPrivate,
                                                String accessKeyFromUser, Long userId) {

        // 1. Find or create session
        CompetitionSession session = competitionSessionService.joinOrCreateSession(
                gameweekId, sessionType, buyInAmount, isPrivate, accessKeyFromUser);

        // 2. Join the session
        return joinSession(session.getId(), userId);
    }

    /**
     * Join an existing session
     */
    @Transactional
    public SessionParticipation joinSession(Long sessionId, Long userId) {
        // Get session and user
        CompetitionSession session = competitionSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate session state
        validateSessionForJoining(session);

        // Check if user already joined this session
        if (sessionParticipationRepository.existsByUserIdAndSessionId(userId, sessionId)) {
            throw new RuntimeException("User already joined this session");
        }

        // Check if user already joined another session for same gameweek/type/amount
        validateUserNotInSimilarSession(userId, session);

        // Check if session is full
        if (session.getCurrentParticipants() >= session.getMaxParticipants()) {
            throw new RuntimeException("Session is full");
        }

        // Create participation record
        SessionParticipation participation = createParticipation(session, user);

        // Update session stats
        updateSessionStats(session);

        // Save everything
        SessionParticipation savedParticipation = sessionParticipationRepository.save(participation);
        competitionSessionRepository.save(session);

        System.out.println("✅ User " + userId + " joined session " + sessionId +
                " (Participants: " + session.getCurrentParticipants() + "/" + session.getMaxParticipants() + ")");

        return savedParticipation;
    }

    /**
     * Leave a session (before it starts)
     */
    @Transactional
    public void leaveSession(Long sessionId, Long userId) {
        SessionParticipation participation = sessionParticipationRepository
                .findByUserIdAndSessionId(userId, sessionId)
                .orElseThrow(() -> new RuntimeException("Participation not found"));

        CompetitionSession session = participation.getSession();

        // Can only leave if session is still open
        if (session.getStatus() != CompetitionSessionStatus.OPEN) {
            throw new RuntimeException("Cannot leave session - it has already started");
        }

        // Update session stats
        session.setCurrentParticipants(session.getCurrentParticipants() - 1);
        session.setTotalPrizePool(session.getTotalPrizePool().subtract(session.getBuyInAmount()));

        // Remove participation
        sessionParticipationRepository.delete(participation);
        competitionSessionRepository.save(session);

        System.out.println("❌ User " + userId + " left session " + sessionId);
    }

    /**
     * Update user's predictions progress
     */
    @Transactional
    public void updatePredictionProgress(Long participationId, int correctPredictions,
                                         int totalPredictions, boolean hasCompletedAll) {
        SessionParticipation participation = sessionParticipationRepository.findById(participationId)
                .orElseThrow(() -> new RuntimeException("Participation not found"));

        participation.setTotalCorrectPredictions(correctPredictions);
        participation.setTotalPredictions(totalPredictions);
        participation.setHasCompletedAllPredictions(hasCompletedAll);

        // Calculate accuracy percentage
        double accuracy = totalPredictions > 0 ?
                (double) correctPredictions / totalPredictions * 100.0 : 0.0;
        participation.setAccuracyPercentage(accuracy);

        sessionParticipationRepository.save(participation);
    }

    /**
     * Get user's participation in a specific session
     */
    public Optional<SessionParticipation> getUserParticipation(Long userId, Long sessionId) {
        return sessionParticipationRepository.findByUserIdAndSessionId(userId, sessionId);
    }

    /**
     * Get user's participations for a gameweek
     */
    public List<SessionParticipation> getUserParticipationsForGameweek(Long userId, Long gameweekId) {
        return sessionParticipationRepository.findByUserIdAndGameweekId(userId, gameweekId);
    }

    /**
     * Get all participations for a session
     */
    public List<SessionParticipation> getSessionParticipations(Long sessionId) {
        return sessionParticipationRepository.findBySessionIdOrderByAccuracyPercentageDesc(sessionId);
    }

    /**
     * Get user's active participations
     */
    public List<SessionParticipation> getUserActiveParticipations(Long userId) {
        return sessionParticipationRepository.findByUserIdAndStatus(userId, ParticipationStatus.ACTIVE);
    }

    /**
     * Check if user can join a session type for a gameweek
     */
    public boolean canUserJoinSession(Long userId, Long gameweekId, SessionType sessionType,
                                      BigDecimal buyInAmount) {
        // Check if user already has an active participation for this gameweek/type/amount
        return !sessionParticipationRepository.existsByUserIdAndGameweekIdAndSessionTypeAndBuyInAmount(
                userId, gameweekId, sessionType, buyInAmount);
    }

    /**
     * Calculate user's total winnings
     */
    public BigDecimal calculateUserTotalWinnings(Long userId) {
        return sessionParticipationRepository.sumPrizeWonByUserId(userId);
    }

    /**
     * Get user's session statistics
     */
    public UserSessionStats getUserSessionStats(Long userId) {
        List<SessionParticipation> participations = sessionParticipationRepository.findByUserId(userId);

        int totalSessions = participations.size();
        int wonSessions = (int) participations.stream()
                .filter(p -> p.getRank() != null && p.getRank() == 1)
                .count();

        BigDecimal totalWinnings = participations.stream()
                .map(SessionParticipation::getPrizeWon)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSpent = participations.stream()
                .map(SessionParticipation::getAmountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        double averageAccuracy = participations.stream()
                .mapToDouble(SessionParticipation::getAccuracyPercentage)
                .average()
                .orElse(0.0);

        return new UserSessionStats(totalSessions, wonSessions, totalWinnings,
                totalSpent, averageAccuracy);
    }

    // ===== PRIVATE HELPER METHODS =====

    private void validateSessionForJoining(CompetitionSession session) {
        if (session.getStatus() != CompetitionSessionStatus.OPEN) {
            throw new RuntimeException("Session is not open for joining");
        }

        if (session.getJoinDeadline() != null && LocalDateTime.now().isAfter(session.getJoinDeadline())) {
            throw new RuntimeException("Join deadline has passed");
        }
    }

    private void validateUserNotInSimilarSession(Long userId, CompetitionSession session) {
        boolean alreadyInSimilarSession = sessionParticipationRepository
                .existsByUserIdAndGameweekIdAndSessionTypeAndBuyInAmount(
                        userId,
                        session.getGameweek().getId(),
                        session.getSessionType(),
                        session.getBuyInAmount()
                );

        if (alreadyInSimilarSession) {
            throw new RuntimeException("User already joined a similar session for this gameweek");
        }
    }

    private SessionParticipation createParticipation(CompetitionSession session, UserEntity user) {
        SessionParticipation participation = new SessionParticipation();
        participation.setUser(user);
        participation.setSession(session);
        participation.setJoinedAt(LocalDateTime.now());
        participation.setAmountPaid(session.getBuyInAmount());
        participation.setStatus(ParticipationStatus.ACTIVE);
        participation.setTotalCorrectPredictions(0);
        participation.setTotalPredictions(0);
        participation.setAccuracyPercentage(0.0);
        participation.setHasCompletedAllPredictions(false);
        participation.setPrizeWon(BigDecimal.ZERO);

        return participation;
    }

    private void updateSessionStats(CompetitionSession session) {
        session.setCurrentParticipants(session.getCurrentParticipants() + 1);
        session.setTotalPrizePool(session.getTotalPrizePool().add(session.getBuyInAmount()));

        // If session is now full, change status
        if (session.getCurrentParticipants() >= session.getMaxParticipants()) {
            session.setStatus(CompetitionSessionStatus.FULL);
        }
    }

    // ===== INNER CLASSES =====

    /**
     * User session statistics
     */
    public static class UserSessionStats {
        private final int totalSessions;
        private final int wonSessions;
        private final BigDecimal totalWinnings;
        private final BigDecimal totalSpent;
        private final double averageAccuracy;

        public UserSessionStats(int totalSessions, int wonSessions, BigDecimal totalWinnings,
                                BigDecimal totalSpent, double averageAccuracy) {
            this.totalSessions = totalSessions;
            this.wonSessions = wonSessions;
            this.totalWinnings = totalWinnings;
            this.totalSpent = totalSpent;
            this.averageAccuracy = averageAccuracy;
        }

        // Getters
        public int getTotalSessions() { return totalSessions; }
        public int getWonSessions() { return wonSessions; }
        public BigDecimal getTotalWinnings() { return totalWinnings; }
        public BigDecimal getTotalSpent() { return totalSpent; }
        public double getAverageAccuracy() { return averageAccuracy; }

        public double getWinRate() {
            return totalSessions > 0 ? (double) wonSessions / totalSessions * 100.0 : 0.0;
        }

        public BigDecimal getNetProfit() {
            return totalWinnings.subtract(totalSpent);
        }
    }
}