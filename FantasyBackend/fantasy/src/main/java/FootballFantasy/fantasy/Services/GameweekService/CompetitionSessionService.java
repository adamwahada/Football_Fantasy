package FootballFantasy.fantasy.Services.GameweekService;

import FootballFantasy.fantasy.Entities.GameweekEntity.*;
import FootballFantasy.fantasy.Entities.UserEntity.UserEntity;
import FootballFantasy.fantasy.Repositories.GameweekRepository.*;
import FootballFantasy.fantasy.Repositories.UserRepository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class CompetitionSessionService {

    @Autowired
    private CompetitionSessionRepository competitionSessionRepository;

    @Autowired
    private SessionTemplateRepository sessionTemplateRepository;

    @Autowired
    private GameWeekRepository gameWeekRepository;

    @Autowired
    private SessionParticipationRepository sessionParticipationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PredictionService predictionService;

    @Autowired
    private MatchRepository matchRepository;

    // Platform fee percentage (10%)
    private static final BigDecimal PLATFORM_FEE_PERCENTAGE = new BigDecimal("0.10");

    /**
     * 🔒Join a session with locking (for actual session creation/joining)
     */
    @Transactional
    public CompetitionSession joinOrCreateSession(Long gameweekId,
                                                  SessionType sessionType,
                                                  BigDecimal buyInAmount,
                                                  boolean isPrivate,
                                                  String accessKeyFromUser,
                                                  LeagueTheme competition) {
        GameWeek gameWeek = gameWeekRepository.findById(gameweekId)
                .orElseThrow(() -> new RuntimeException("GameWeek not found"));

        SessionTemplate template = sessionTemplateRepository
                .findByCompetitionAndSessionTypeAndIsActiveTrue(competition, sessionType)
                .stream()
                .filter(t -> t.getBuyInAmount().equals(buyInAmount) && t.getIsPrivate().equals(isPrivate))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No active template for that competition/type/amount"));

        CompetitionSession session;

        if (isPrivate) {
            if (accessKeyFromUser != null && !accessKeyFromUser.isBlank()) {
                // JOIN existing private session by access key (with lock)
                session = competitionSessionRepository.findPrivateSessionByAccessKeyWithLock(accessKeyFromUser, competition)
                        .orElseThrow(() -> new RuntimeException("Private session not found, full, or invalid access key"));
            } else {
                // CREATE a new private session with a new unique access key
                session = createNewSessionFromTemplate(gameWeek, template, true, competition);
            }
        } else {
            // Public session logic (reuse or create) - with lock
            session = competitionSessionRepository.findAvailableSessionWithLock(gameweekId, competition, sessionType, buyInAmount)
                    .orElse(null);
            if (session == null) {
                session = createNewSessionFromTemplate(gameWeek, template, false, competition);
            }
        }

        return session;
    }

    /**
     * 🏗️ Create actual session (saved to DB)
     */
    public CompetitionSession createNewSessionFromTemplate(GameWeek gameWeek,
                                                           SessionTemplate template,
                                                           boolean isPrivate,
                                                           LeagueTheme competition) {
        if (gameWeek.getJoinDeadline() == null) {
            throw new RuntimeException("GameWeek join deadline not set");
        }

        CompetitionSession session = new CompetitionSession();
        session.setGameweek(gameWeek);
        session.setCompetition(competition);
        session.setSessionName(template.getTemplateName());
        session.setSessionType(template.getSessionType());
        session.setBuyInAmount(template.getBuyInAmount());
        session.setMaxParticipants(template.getMaxParticipants());
        session.setCurrentParticipants(0);
        session.setStatus(CompetitionSessionStatus.OPEN);
        session.setCreatedAt(LocalDateTime.now());
        session.setJoinDeadline(gameWeek.getJoinDeadline());
        session.setTotalPrizePool(BigDecimal.ZERO);

        if (isPrivate) {
            session.setAccessKey(UUID.randomUUID().toString().substring(0,8));
        }

        return competitionSessionRepository.save(session);
    }

    // 🏆 Determine Winner and Assign Ranks with Balance Update

    // 🏆 Determine Winner and Assign Ranks with Balance Update
    @Transactional
    public void determineWinner(Long sessionId) {
        CompetitionSession session = competitionSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Competition session not found"));

        List<SessionParticipation> participations = session.getParticipations();

        if (participations.isEmpty()) {
            throw new RuntimeException("No participants found in this session");
        }

        // 🚨 SPECIAL CASE: One-vs-One with only 1 participant = REFUND
        if (session.getSessionType() == SessionType.ONE_VS_ONE && participations.size() == 1) {
            handleOneVsOneRefund(session, participations.get(0));
            return;
        }

        // 🚨 SPECIAL CASE: Any session with only 1 participant = REFUND
        if (participations.size() == 1) {
            handleSingleParticipantRefund(session, participations.get(0));
            return;
        }

        // 1. Calculate accuracy for all participants first
        for (SessionParticipation participation : participations) {
            predictionService.calculatePredictionAccuracy(participation.getId());
        }

        // 2. Enhanced sorting with tiebreaker
        participations.sort(Comparator
                .comparingDouble(SessionParticipation::getAccuracyPercentage).reversed()
                .thenComparingDouble(p -> predictionService.getTiebreakerScore(p.getId()))
                .thenComparingInt(SessionParticipation::getTotalCorrectPredictions).reversed()
                .thenComparing(SessionParticipation::getJoinedAt));

        // 3. Calculate prize amounts (with platform fee)
        BigDecimal totalPrizePool = session.getTotalPrizePool();
        BigDecimal platformFee = totalPrizePool.multiply(PLATFORM_FEE_PERCENTAGE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal winnerPrize = totalPrizePool.subtract(platformFee);

        // 4. Assign ranks and update winner's balance
        int ranking = 1;
        SessionParticipation winner = null;

        for (SessionParticipation p : participations) {
            p.setRanking(ranking);
            if (ranking == 1) {
                // Winner gets prize minus platform fee
                p.setPrizeWon(winnerPrize);
                p.setIsWinner(true);
                winner = p;
                session.setWinner(p.getUser());
                session.setStatus(CompetitionSessionStatus.FINISHED);
            } else {
                // Others get nothing
                p.setPrizeWon(BigDecimal.ZERO);
                p.setIsWinner(false);
            }
            ranking++;
        }

        // 5. Update winner's balance in their account
        if (winner != null) {
            UserEntity winnerUser = winner.getUser();
            BigDecimal currentBalance = winnerUser.getBalance();
            BigDecimal newBalance = currentBalance.add(winnerPrize);
            winnerUser.setBalance(newBalance);

            // Save the updated user balance
            userRepository.save(winnerUser);

            System.out.println("✅ Winner " + winnerUser.getId() + " received prize: " + winnerPrize +
                    " (Platform fee: " + platformFee + ")");
            System.out.println("💰 Winner's balance updated from " + currentBalance + " to " + newBalance);
        }

        // 6. Persist all changes
        sessionParticipationRepository.saveAll(participations);
        competitionSessionRepository.save(session);
    }
    // 🔄 Handle One-vs-One Refund (NO platform fee - full refund)
    @Transactional
    public void handleOneVsOneRefund(CompetitionSession session, SessionParticipation onlyParticipant) {
        System.out.println("🔄 One-vs-One session with only 1 participant - issuing full refund");

        BigDecimal refundAmount = onlyParticipant.getAmountPaid(); // Full amount back

        // Update participant
        onlyParticipant.setPrizeWon(refundAmount);
        onlyParticipant.setIsWinner(true); // They "win" by default but get refund
        onlyParticipant.setRanking(1);

        // Update session
        session.setWinner(onlyParticipant.getUser());
        session.setStatus(CompetitionSessionStatus.CANCELLED); // Different status for refunds

        // Refund user's balance (full amount, no platform fee)
        UserEntity user = onlyParticipant.getUser();
        BigDecimal currentBalance = user.getBalance();
        BigDecimal newBalance = currentBalance.add(refundAmount);
        user.setBalance(newBalance);

        userRepository.save(user);
        sessionParticipationRepository.save(onlyParticipant);
        competitionSessionRepository.save(session);

        System.out.println("✅ Refund issued: " + refundAmount + " to user " + user.getId());
        System.out.println("💰 User's balance updated from " + currentBalance + " to " + newBalance);
    }

    // 🔄 Handle Any Single Participant Refund
    @Transactional
    public void handleSingleParticipantRefund(CompetitionSession session, SessionParticipation onlyParticipant) {
        System.out.println("🔄 Session with only 1 participant - issuing full refund");

        BigDecimal refundAmount = onlyParticipant.getAmountPaid(); // Full amount back

        // Update participant
        onlyParticipant.setPrizeWon(refundAmount);
        onlyParticipant.setIsWinner(true);
        onlyParticipant.setRanking(1);

        // Update session
        session.setWinner(onlyParticipant.getUser());
        session.setStatus(CompetitionSessionStatus.CANCELLED);

        // Refund user's balance
        UserEntity user = onlyParticipant.getUser();
        BigDecimal currentBalance = user.getBalance();
        BigDecimal newBalance = currentBalance.add(refundAmount);
        user.setBalance(newBalance);

        userRepository.save(user);
        sessionParticipationRepository.save(onlyParticipant);
        competitionSessionRepository.save(session);

        System.out.println("✅ Single participant refund: " + refundAmount + " to user " + user.getId());
        System.out.println("💰 User's balance updated from " + currentBalance + " to " + newBalance);
    }


    // Automatic winner determination after matches are finished
    @Transactional
    public void determineWinnersForCompletedGameWeek(Long gameWeekId) {
        List<CompetitionSession> sessions = competitionSessionRepository.findByGameweekId(gameWeekId);

        for (CompetitionSession session : sessions) {
            if (session.getStatus() != CompetitionSessionStatus.FINISHED) {
                try {
                    determineWinner(session.getId()); // Use the main method
                    System.out.println("✅ Winner determined for session " + session.getId());
                } catch (Exception e) {
                    System.err.println("❌ Error determining winner for session " + session.getId() + ": " + e.getMessage());
                    e.printStackTrace(); // Add stack trace for debugging
                }
            }
        }
    }

    /**
     * 🔐 Admin-triggered fallback to manually determine winners for a GameWeek.
     */
    @Transactional
    public void manuallyTriggerWinnerDetermination(Long gameWeekId) {
        List<CompetitionSession> sessions = competitionSessionRepository.findByGameweekId(gameWeekId);

        if (sessions.isEmpty()) {
            throw new RuntimeException("No competition sessions found for GameWeek ID: " + gameWeekId);
        }

        int processedSessions = 0;
        for (CompetitionSession session : sessions) {
            if (session.getStatus() != CompetitionSessionStatus.FINISHED) {
                try {
                    determineWinner(session.getId());
                    processedSessions++;
                    System.out.println("✅ Manually determined winner for session " + session.getId());
                } catch (Exception e) {
                    System.err.println("❌ Error manually determining winner for session " + session.getId() + ": " + e.getMessage());
                }
            }
        }

        System.out.println("🎯 Manual winner determination completed. Processed " + processedSessions + " sessions.");
    }

    /**
     * Get session prize breakdown for transparency
     */
    public SessionPrizeBreakdown getSessionPrizeBreakdown(Long sessionId) {
        CompetitionSession session = competitionSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Competition session not found"));

        BigDecimal totalPrizePool = session.getTotalPrizePool();
        BigDecimal platformFee = totalPrizePool.multiply(PLATFORM_FEE_PERCENTAGE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal winnerPrize = totalPrizePool.subtract(platformFee);

        return new SessionPrizeBreakdown(totalPrizePool, platformFee, winnerPrize);
    }

    /**
     * Get session by ID
     */
    public CompetitionSession getSessionById(Long sessionId) {
        return competitionSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Competition session not found"));
    }

    // Inner class for prize breakdown
    public static class SessionPrizeBreakdown {
        private final BigDecimal totalPrizePool;
        private final BigDecimal platformFee;
        private final BigDecimal winnerPrize;

        public SessionPrizeBreakdown(BigDecimal totalPrizePool, BigDecimal platformFee, BigDecimal winnerPrize) {
            this.totalPrizePool = totalPrizePool;
            this.platformFee = platformFee;
            this.winnerPrize = winnerPrize;
        }

        public BigDecimal getTotalPrizePool() { return totalPrizePool; }
        public BigDecimal getPlatformFee() { return platformFee; }
        public BigDecimal getWinnerPrize() { return winnerPrize; }
    }
    public List<CompetitionSession> findExpiredOpenSessions(LocalDateTime now) {
        return competitionSessionRepository.findByStatusAndJoinDeadlineBefore(
                CompetitionSessionStatus.OPEN, now);
    }
    @Transactional
    public void cancelEmptySession(Long sessionId) {
        CompetitionSession session = competitionSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found"));

        session.setStatus(CompetitionSessionStatus.CANCELLED);
        competitionSessionRepository.save(session);

        System.out.println("🚫 Cancelled empty session: " + sessionId);
    }
}