package FootballFantasy.fantasy.Services.GameweekService;

import FootballFantasy.fantasy.Entities.GameweekEntity.*;
import FootballFantasy.fantasy.Entities.UserEntity.UserEntity;
import FootballFantasy.fantasy.Exception.BusinessLogicException;
import FootballFantasy.fantasy.Repositories.GameweekRepository.*;
import FootballFantasy.fantasy.Repositories.UserRepository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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

    // Platform fee percentage (10%)
    private static final BigDecimal PLATFORM_FEE_PERCENTAGE = new BigDecimal("0.10");

    @Transactional
    public CompetitionSession joinOrCreateSession(Long gameweekId,
                                                  SessionType sessionType,
                                                  BigDecimal buyInAmount,
                                                  boolean isPrivate,
                                                  boolean isCreatingPrivateSession,
                                                  String accessKeyFromUser,
                                                  LeagueTheme competition) {

        // 1️⃣ Load GameWeek
        GameWeek gameWeek = gameWeekRepository.findById(gameweekId)
                .orElseThrow(() -> new BusinessLogicException(
                        "GameWeek not found",
                        "GAMEWEEK_NOT_FOUND"
                ));

        // 2️⃣ Load active template for this competition/type/amount/privacy
        SessionTemplate template = sessionTemplateRepository
                .findActiveTemplateByCompetitionTypeAmountAndPrivacy(
                        competition, sessionType, buyInAmount, isPrivate)
                .orElseThrow(() -> new BusinessLogicException(
                        "No active template for that competition/type/amount/private",
                        "SESSION_TEMPLATE_NOT_FOUND"
                ));

        CompetitionSession session;

        if (isPrivate) {

            if (isCreatingPrivateSession) {
                // 3️⃣ Determine key to use for new private session
                String keyToUse;

                if (accessKeyFromUser != null && !accessKeyFromUser.trim().isEmpty()) {
                    // Check if key already exists anywhere for same competition
                    Optional<CompetitionSession> existing = competitionSessionRepository
                            .findPrivateSessionByAccessKeyAnyGameweek(accessKeyFromUser.trim(), competition);

                    if (existing.isPresent()) {
                        // Key exists → generate new unique key
                        System.out.println("⚠️ Frontend key already exists, generating new backend key");
                        keyToUse = ensureUniqueAccessKeyForCompetition(competition);
                    } else {
                        // Safe to use frontend key
                        keyToUse = accessKeyFromUser.trim();
                    }
                } else {
                    // No frontend key → generate backend key
                    keyToUse = ensureUniqueAccessKeyForCompetition(competition);
                }

                // 4️⃣ Create the private session
                session = createNewSessionFromTemplate(gameWeek, template, true, competition, keyToUse);

            } else {
                // 5️⃣ Join existing private session by access key
                System.out.println("🔗 Joining existing private session with key: " + accessKeyFromUser);
                session = competitionSessionRepository
                        .findPrivateSessionByAccessKeyWithLock(accessKeyFromUser.trim(), competition, gameweekId)
                        .orElseThrow(() -> new BusinessLogicException(
                                "Private session not found with the given access key for this gameweek/competition",
                                "PRIVATE_SESSION_NOT_FOUND"
                        ));
            }

        } else {
            // 6️⃣ Public session: try to join existing, otherwise create new
            session = competitionSessionRepository
                    .findAvailableSessionWithLock(gameweekId, competition, sessionType, buyInAmount)
                    .orElseGet(() -> createNewSessionFromTemplate(gameWeek, template, false, competition));
        }

        return session;
    }

    private String generateUniqueAccessKey() {
        // Basic token; prefer ensureUniqueAccessKeyForCompetition when competition is known
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private String generateAccessKeyToken() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private String ensureUniqueAccessKeyForCompetition(LeagueTheme competition) {
        // Try a few candidates to avoid SQL unique constraint violations
        for (int i = 0; i < 5; i++) {
            String candidate = generateAccessKeyToken();
            Optional<CompetitionSession> existing = competitionSessionRepository
                    .findPrivateSessionByAccessKeyAnyGameweek(candidate, competition);
            if (existing.isEmpty()) {
                return candidate;
            }
        }
        // As a last resort, return a token; DB constraint will still protect
        return generateAccessKeyToken();
    }


    /**
     * 🏗️ Create actual session (saved to DB)
     */
    public CompetitionSession createNewSessionFromTemplate(GameWeek gameWeek,
                                                           SessionTemplate template,
                                                           boolean isPrivate,
                                                           LeagueTheme competition) {
        return createNewSessionFromTemplate(gameWeek, template, isPrivate, competition, null);
    }

    // ✅ Single source of truth
    public CompetitionSession createNewSessionFromTemplate(GameWeek gameWeek,
                                                           SessionTemplate template,
                                                           boolean isPrivate,
                                                           LeagueTheme competition,
                                                           String desiredAccessKey) {
        if (gameWeek.getJoinDeadline() == null) {
            throw new BusinessLogicException(
                    "GameWeek join deadline not set",
                    "JOIN_DEADLINE_NOT_SET"
            );
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
            String accessKey;
            if (desiredAccessKey != null && !desiredAccessKey.isBlank()) {
                accessKey = desiredAccessKey.trim();
                // Ensure uniqueness across competition
                Optional<CompetitionSession> existing = competitionSessionRepository
                        .findPrivateSessionByAccessKeyAnyGameweek(accessKey, competition);
                if (existing.isPresent()) {
                    System.out.println("⚠️ Desired access key already exists for competition. Generating unique key.");
                    accessKey = ensureUniqueAccessKeyForCompetition(competition);
                }
            } else {
                accessKey = ensureUniqueAccessKeyForCompetition(competition);
            }

            // Enforce max 8 chars
            if (accessKey.length() > 8) {
                accessKey = accessKey.substring(0, 8).toUpperCase();
            }

            session.setAccessKey(accessKey);
        }

        try {
            return competitionSessionRepository.save(session);
        } catch (DataIntegrityViolationException ex) {
            // Handle rare race or key used in another competition/gameweek (global unique index)
            if (isPrivate && session.getAccessKey() != null) {
                System.out.println("⚠️ Unique constraint hit for access key '" + session.getAccessKey() + "'. Retrying with a new key.");
                // Retry once with a fresh unique key
                String newKey = ensureUniqueAccessKeyForCompetition(session.getCompetition());
                session.setAccessKey(newKey);
                try {
                    return competitionSessionRepository.save(session);
                } catch (DataIntegrityViolationException ex2) {
                    throw new BusinessLogicException(
                            "Access key is already taken. Please try again.",
                            "ACCESS_KEY_CONFLICT"
                    );
                }
            }
            throw ex;
        }
    }

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