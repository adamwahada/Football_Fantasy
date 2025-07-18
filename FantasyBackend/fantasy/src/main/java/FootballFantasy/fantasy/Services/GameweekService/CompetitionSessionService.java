package FootballFantasy.fantasy.Services.GameweekService;

import FootballFantasy.fantasy.Entities.GameweekEntity.*;
import FootballFantasy.fantasy.Repositories.GameweekRepository.CompetitionSessionRepository;
import FootballFantasy.fantasy.Repositories.GameweekRepository.GameWeekRepository;
import FootballFantasy.fantasy.Repositories.GameweekRepository.SessionParticipationRepository;
import FootballFantasy.fantasy.Repositories.GameweekRepository.SessionTemplateRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

    /*** Join a session (public or private) based on template.
     * If no available session is found, create a new one from template. */
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
                .filter(t -> t.getBuyInAmount().equals(buyInAmount))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No active template for that competition/type/amount"));

        CompetitionSession session = null;

        if (isPrivate) {
            if (accessKeyFromUser != null && !accessKeyFromUser.isBlank()) {
                session = competitionSessionRepository
                        .findPrivateSessionByAccessKey(accessKeyFromUser, competition);
            }
            if (session == null) {
                session = createNewSessionFromTemplate(gameWeek, template, true);
            }
        } else {
            session = competitionSessionRepository
                    .findAvailableSession(gameweekId, competition, sessionType, buyInAmount);
            if (session == null) {
                session = createNewSessionFromTemplate(gameWeek, template, false);
            }
        }

        return session;
    }

    public CompetitionSession createNewSessionFromTemplate(GameWeek gameWeek,
                                                           SessionTemplate template,
                                                           boolean isPrivate) {
        if (gameWeek.getJoinDeadline() == null) {
            throw new RuntimeException("GameWeek join deadline not set");
        }

        CompetitionSession session = new CompetitionSession();
        session.setGameweek(gameWeek);
        session.setCompetition(gameWeek.getCompetition());          // 🎯 Tie to competition
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


    // 🏆 Determine Winner and Assign Ranks
    @Transactional
    public void determineWinner(Long sessionId) {
        CompetitionSession session = competitionSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Competition session not found"));

        List<SessionParticipation> participations = session.getParticipations();

        if (participations.isEmpty()) {
            throw new RuntimeException("No participants found in this session");
        }

        // 1. Sort by accuracy (descending)
        participations.sort(Comparator.comparingDouble(SessionParticipation::getAccuracyPercentage).reversed());

        // 2. Assign ranks and calculate prize
        BigDecimal prizePool = session.getTotalPrizePool();
        int rank = 1;

        for (SessionParticipation p : participations) {
            p.setRank(rank);
            if (rank == 1) {
                // Winner gets full prize
                p.setPrizeWon(prizePool);
                session.setWinner(p.getUser());
                session.setStatus(CompetitionSessionStatus.FINISHED);
            } else {
                // Others get nothing (custom logic if needed)
                p.setPrizeWon(BigDecimal.ZERO);
            }
            rank++;
        }

        // 3. Persist changes
        sessionParticipationRepository.saveAll(participations);
        competitionSessionRepository.save(session);
    }

    //Automatic winner declenchement after the matches are finished
    @Transactional
    public void determineWinnersForCompletedGameWeek(Long gameWeekId) {
        List<CompetitionSession> sessions = competitionSessionRepository.findByGameweekId(gameWeekId);

        for (CompetitionSession session : sessions) {
            if (session.getStatus() != CompetitionSessionStatus.FINISHED) {
                determineWinner(session.getId());
            }
        }
    }
    /*** 🔐 Admin-triggered fallback to manually determine winners for a GameWeek.
     * Useful when some matches are delayed, manually marked completed */
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
