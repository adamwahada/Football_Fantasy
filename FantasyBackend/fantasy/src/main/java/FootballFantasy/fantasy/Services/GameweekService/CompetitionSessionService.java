package FootballFantasy.fantasy.Services.GameweekService;

import FootballFantasy.fantasy.Entities.GameweekEntity.*;
import FootballFantasy.fantasy.Repositories.GameweekRepository.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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
    private PredictionService predictionService;

    @Autowired
    private MatchRepository matchRepository;

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

    // 🏆 Determine Winner and Assign Ranks (FIXED)
    @Transactional
    public void determineWinner(Long sessionId) {
        CompetitionSession session = competitionSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Competition session not found"));

        List<SessionParticipation> participations = session.getParticipations();

        if (participations.isEmpty()) {
            throw new RuntimeException("No participants found in this session");
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

        // 3. Assign ranks and calculate prize
        BigDecimal prizePool = session.getTotalPrizePool();
        int ranking = 1;

        for (SessionParticipation p : participations) {
            p.setRanking(ranking);
            if (ranking == 1) {
                // Winner gets full prize
                p.setPrizeWon(prizePool);
                session.setWinner(p.getUser());
                session.setStatus(CompetitionSessionStatus.FINISHED);
            } else {
                // Others get nothing (custom logic if needed)
                p.setPrizeWon(BigDecimal.ZERO);
            }
            ranking++;
        }

        // 4. Persist changes
        sessionParticipationRepository.saveAll(participations);
        competitionSessionRepository.save(session);
    }

    // Automatic winner determination after matches are finished
    @Transactional
    public void determineWinnersForCompletedGameWeek(Long gameWeekId) {
        List<CompetitionSession> sessions = competitionSessionRepository.findByGameweekId(gameWeekId);

        for (CompetitionSession session : sessions) {
            if (session.getStatus() != CompetitionSessionStatus.FINISHED) {
                determineWinner(session.getId());
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

        for (CompetitionSession session : sessions) {
            if (session.getStatus() != CompetitionSessionStatus.FINISHED) {
                determineWinner(session.getId());
            }
        }
    }
}