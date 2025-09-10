package FootballFantasy.fantasy.Services.GameweekService;

import FootballFantasy.fantasy.Dto.UserSessionStats;
import FootballFantasy.fantasy.Entities.GameweekEntity.*;
import FootballFantasy.fantasy.Entities.UserEntity.UserEntity;
import FootballFantasy.fantasy.Exception.BusinessLogicException;
import FootballFantasy.fantasy.Exception.InsufficientBalanceException;
import FootballFantasy.fantasy.Repositories.GameweekRepository.CompetitionSessionRepository;
import FootballFantasy.fantasy.Repositories.GameweekRepository.GameWeekRepository;
import FootballFantasy.fantasy.Repositories.GameweekRepository.PredictionRepository;
import FootballFantasy.fantasy.Repositories.GameweekRepository.SessionParticipationRepository;
import FootballFantasy.fantasy.Repositories.UserRepository.UserRepository;
import FootballFantasy.fantasy.Services.UserService.UserService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private PredictionRepository predictionRepository;

    @Autowired
    private GameWeekRepository gameWeekRepository;

    @Autowired
    @Lazy
    private CompetitionSessionService competitionSessionService;

    /**
     * Complete flow: Find/Create session and join it
     */
    @Transactional
    public SessionParticipation joinCompetition(Long gameweekId,
                                                LeagueTheme competitionFromFrontend, // still keep this param if needed for validation
                                                SessionType sessionType,
                                                BigDecimal buyInAmount,
                                                boolean isPrivate,
                                                String accessKeyFromUser,
                                                Long userId) {

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessLogicException("User not found", "USER_NOT_FOUND"));

        validateUserHasBalance(user, buyInAmount);

        // ✅ Get the actual GameWeek and real competition from DB
        GameWeek gameWeek = gameWeekRepository.findById(gameweekId)
                .orElseThrow(() -> new BusinessLogicException("GameWeek not found", "GAMEWEEK_NOT_FOUND"));

        LeagueTheme actualCompetition = gameWeek.getCompetition();

        // ❌ Optional validation (but recommended)
        if (!actualCompetition.equals(competitionFromFrontend)) {
            throw new BusinessLogicException("Mismatch between GameWeek's competition and provided competition", "COMPETITION_MISMATCH");
        }

        // ✅ Always use the correct competition from GameWeek
        CompetitionSession session = competitionSessionService
                .joinOrCreateSession(gameweekId, sessionType, buyInAmount, isPrivate, accessKeyFromUser, actualCompetition);

        return joinSession(session.getId(), userId);
    }


    /**
     * Complete flow: Find/Create session and join it (using Keycloak ID)
     */
    @Transactional
    public SessionParticipation joinCompetitionByKeycloakId(Long gameweekId, LeagueTheme competitionFromFrontend,
                                                            SessionType sessionType,
                                                            BigDecimal buyInAmount,
                                                            boolean isPrivate, String accessKeyFromUser, String keycloakId) {
        if (keycloakId == null || keycloakId.isBlank()) {
            throw new BusinessLogicException("Keycloak ID is required", "KEYCLOAK_ID_REQUIRED");
        }

        UserEntity user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new BusinessLogicException("User not found with Keycloak ID: " + keycloakId, "USER_NOT_FOUND"));
        validateUserHasBalance(user, buyInAmount);

        return joinCompetition(gameweekId, competitionFromFrontend, sessionType, buyInAmount,
                isPrivate, accessKeyFromUser, user.getId()
        );
    }


    /**
     * Join an existing session
     */
    @Transactional
    public SessionParticipation joinSession(Long sessionId, Long userId) {
        // Get session and user
        CompetitionSession session = competitionSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessLogicException("Session not found", "SESSION_NOT_FOUND"));

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessLogicException("User not found", "USER_NOT_FOUND"));

        // Validate session state
        validateSessionForJoining(session);
        // Validate user eligibility (terms, profile, etc.)
        validateUserEligibility(user);
        // ✅ Balance check
        validateUserHasBalance(user, session.getBuyInAmount());

        // Check if user already joined this session
        if (sessionParticipationRepository.existsByUserIdAndSessionId(userId, sessionId)) {
            throw new BusinessLogicException("User already joined this session", "ALREADY_JOINED");
        }

        // Check if session is full
        if (session.getCurrentParticipants() >= session.getMaxParticipants()) {
            throw new BusinessLogicException("Session is full", "SESSION_FULL");
        }

        // Create participation record
        SessionParticipation participation = createParticipation(session, user);

        // Update session stats
        updateSessionStats(session);
        user.setBalance(user.getBalance().subtract(session.getBuyInAmount()));
        userRepository.save(user); // ✅ Add this to save user balance

        // Save everything
        SessionParticipation savedParticipation = sessionParticipationRepository.save(participation);
        competitionSessionRepository.save(session);

        System.out.println("✅ User " + userId + " joined session " + sessionId +
                " (Participants: " + session.getCurrentParticipants() + "/" + session.getMaxParticipants() + ")");

        return savedParticipation;
    }

    /**
     * Join an existing session using Keycloak ID
     */
    @Transactional
    public SessionParticipation joinSessionByKeycloakId(Long sessionId, String keycloakId) {
        UserEntity user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new BusinessLogicException("User not found with Keycloak ID: " + keycloakId, "USER_NOT_FOUND"));

        return joinSession(sessionId, user.getId());
    }

    /**
     * Leave a session (before it starts)
     */
    @Transactional
    public void leaveSession(Long sessionId, Long userId) {
        SessionParticipation participation = sessionParticipationRepository
                .findByUserIdAndSessionId(userId, sessionId)
                .orElseThrow(() -> new BusinessLogicException("Participation not found", "PARTICIPATION_NOT_FOUND"));

        CompetitionSession session = participation.getSession();

        // Can only leave if session is still open
        if (session.getStatus() != CompetitionSessionStatus.OPEN) {
            throw new BusinessLogicException("Cannot leave session - it has already started", "SESSION_ALREADY_STARTED");
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessLogicException("User not found", "USER_NOT_FOUND"));

        // Refund the user
        user.setBalance(user.getBalance().add(session.getBuyInAmount()));
        userRepository.save(user);

        // Update session stats
        session.setCurrentParticipants(session.getCurrentParticipants() - 1);
        session.setTotalPrizePool(session.getTotalPrizePool().subtract(session.getBuyInAmount()));

        // If session becomes available again, change status back to OPEN
        if (session.getStatus() == CompetitionSessionStatus.FULL &&
                session.getCurrentParticipants() < session.getMaxParticipants()) {
            session.setStatus(CompetitionSessionStatus.OPEN);
        }

        // Remove participation
        sessionParticipationRepository.delete(participation);
        competitionSessionRepository.save(session);

        System.out.println("❌ User " + userId + " left session " + sessionId);
    }

    /**
     * Leave a session using Keycloak ID
     */
    @Transactional
    public void leaveSessionByKeycloakId(Long sessionId, String keycloakId) {
        UserEntity user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new BusinessLogicException("User not found with Keycloak ID: " + keycloakId, "USER_NOT_FOUND"));

        leaveSession(sessionId, user.getId());
    }

    /**
     * Update user's predictions progress
     */
    @Transactional
    public void updatePredictionProgress(Long participationId, int correctPredictions,
                                         int totalPredictions, boolean hasCompletedAll) {
        SessionParticipation participation = sessionParticipationRepository.findById(participationId)
                .orElseThrow(() -> new BusinessLogicException("Participation not found", "PARTICIPATION_NOT_FOUND"));

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
     * Get user's participation in a specific session by Keycloak ID
     */
    public Optional<SessionParticipation> getUserParticipationByKeycloakId(String keycloakId, Long sessionId) {
        UserEntity user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new BusinessLogicException("User not found with Keycloak ID: " + keycloakId, "USER_NOT_FOUND"));

        return sessionParticipationRepository.findByUserIdAndSessionId(user.getId(), sessionId);
    }

    /**
     * Get user's participations for a gameweek
     */
    public List<SessionParticipation> getUserParticipationsForGameweek(Long userId, Long gameweekId) {
        return sessionParticipationRepository.findByUserIdAndGameweekId(userId, gameweekId);
    }

    /**
     * Get user's participations for a gameweek by Keycloak ID
     */
    public List<SessionParticipation> getUserParticipationsForGameweekByKeycloakId(String keycloakId, Long gameweekId) {
        UserEntity user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new BusinessLogicException("User not found with Keycloak ID: " + keycloakId, "USER_NOT_FOUND"));

        return sessionParticipationRepository.findByUserIdAndGameweekId(user.getId(), gameweekId);
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
     * Get user's active participations by Keycloak ID
     */
    public List<SessionParticipation> getUserActiveParticipationsByKeycloakId(String keycloakId) {
        UserEntity user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new BusinessLogicException("User not found with Keycloak ID: " + keycloakId, "USER_NOT_FOUND"));

        return sessionParticipationRepository.findByUserIdAndStatus(user.getId(), ParticipationStatus.ACTIVE);
    }

    /**
     * Check if user can join a session type for a gameweek
     */
    public boolean canUserJoinSession(Long userId, Long gameweekId, SessionType sessionType,
                                      BigDecimal buyInAmount, LeagueTheme competition) {
        // Check if user already has an active participation for this gameweek/type/amount
        boolean hasParticipation = sessionParticipationRepository.existsByUserIdAndGameweekIdAndSessionTypeAndBuyInAmountAndSession_Competition(
                userId, gameweekId, sessionType, buyInAmount, competition);

        if (hasParticipation) {
            return false;
        }

        // Also check if user has predictions for this combination (edge case)
        boolean hasPredictions = predictionRepository.existsByUserAndGameweekSession(
                userId, gameweekId, sessionType, buyInAmount, competition);

        return !hasPredictions;
    }

    /**
     * Check if user can join a session type for a gameweek by Keycloak ID
     */
    public boolean canUserJoinSessionByKeycloakId(String keycloakId, Long gameweekId,
                                                  SessionType sessionType, BigDecimal buyInAmount,LeagueTheme competition) {
        UserEntity user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new BusinessLogicException("User not found with Keycloak ID: " + keycloakId, "USER_NOT_FOUND"));

        return canUserJoinSession(user.getId(), gameweekId, sessionType, buyInAmount,competition);
    }

    /**
     * Calculate user's total winnings
     */
    public BigDecimal calculateUserTotalWinnings(Long userId) {
        return sessionParticipationRepository.sumPrizeWonByUserId(userId);
    }

    /**
     * Calculate user's total winnings by Keycloak ID
     */
    public BigDecimal calculateUserTotalWinningsByKeycloakId(String keycloakId) {
        UserEntity user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new BusinessLogicException("User not found with Keycloak ID: " + keycloakId, "USER_NOT_FOUND"));

        return calculateUserTotalWinnings(user.getId());
    }

    /**
     * Get user's session statistics
     */
    public UserSessionStats getUserSessionStats(Long userId) {
        List<SessionParticipation> participations = sessionParticipationRepository.findByUserId(userId);

        int totalSessions = participations.size();
        int wonSessions = (int) participations.stream()
                .filter(p -> p.getRanking() != null && p.getRanking() == 1)
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

        // ✅ ADD THESE:
        BigDecimal netProfit = totalWinnings.subtract(totalSpent);
        double winRate = totalSessions > 0 ? (double) wonSessions / totalSessions * 100.0 : 0.0;

        return new UserSessionStats(
                totalSessions,
                wonSessions,
                totalWinnings,
                totalSpent,
                averageAccuracy,
                netProfit,
                winRate
        );
    }

    /**
     * Get user's session statistics by Keycloak ID
     */
    public UserSessionStats getUserSessionStatsByKeycloakId(String keycloakId) {
        UserEntity user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new BusinessLogicException("User not found with Keycloak ID: " + keycloakId, "USER_NOT_FOUND"));

        return getUserSessionStats(user.getId());
    }

    // ===== PRIVATE HELPER METHODS =====

    private void validateSessionForJoining(CompetitionSession session) {
        if (session.getStatus() != CompetitionSessionStatus.OPEN) {
            throw new BusinessLogicException(
                    "Session is not open for joining",
                    "SESSION_NOT_OPEN"
            );
        }

        if (session.getJoinDeadline() != null && LocalDateTime.now().isAfter(session.getJoinDeadline())) {
            throw new BusinessLogicException(
                    "Join deadline has passed",
                    "DEADLINE_PASSED"
            );
        }
    }

    /**
     * Validate user eligibility to join sessions
     */
    private void validateUserEligibility(UserEntity user) {
        // Terms & conditions check
        if (!user.isTermsAccepted()) {
            throw new BusinessLogicException(
                    "User must accept terms and conditions before joining sessions",
                    "TERMS_NOT_ACCEPTED"
            );
        }


        // Permanent ban
        if (!Boolean.TRUE.equals(user.isActive())) {
            throw new BusinessLogicException(
                    "User is permanently banned",
                    "USER_PERMANENTLY_BANNED"
            );
        }

        // Temporary ban
        if (user.getBannedUntil() != null && user.getBannedUntil().isAfter(LocalDate.now())) {
            throw new BusinessLogicException(
                    "User is temporarily banned until " + user.getBannedUntil(),
                    "USER_TEMPORARILY_BANNED"
            );
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

    private void validateUserHasBalance(UserEntity user, BigDecimal buyInAmount) {
        if (user.getBalance().compareTo(buyInAmount) < 0) {
            throw new InsufficientBalanceException(
                    user.getId().toString(),
                    buyInAmount.toString(),
                    user.getBalance().toString()
            );
        }
    }
}