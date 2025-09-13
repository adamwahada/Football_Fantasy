package FootballFantasy.fantasy.Services.GameweekService;

import FootballFantasy.fantasy.Entities.GameweekEntities.*;
import FootballFantasy.fantasy.Repositories.GameweekRepositories.CompetitionSessionRepository;
import FootballFantasy.fantasy.Repositories.GameweekRepositories.SessionParticipationRepository;
import FootballFantasy.fantasy.Services.UserService.UserService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class SessionCleanupScheduler {

    @Autowired
    private CompetitionSessionRepository sessionRepository;

    @Autowired
    private SessionParticipationRepository participationRepository;

    @Autowired
    private UserService userService;

    @Scheduled(fixedDelay = 60_000) // every minute
    @Transactional
    public void refundExpired1v1Sessions() {
        List<CompetitionSession> expiredSessions = sessionRepository
                .findByStatusAndSessionTypeAndMaxParticipantsAndJoinDeadlineBefore(
                        CompetitionSessionStatus.OPEN,
                        SessionType.ONE_VS_ONE,
                        2,
                        LocalDateTime.now()
                );

        for (CompetitionSession session : expiredSessions) {
            List<SessionParticipation> participations = participationRepository.findBySessionIdOrderByAccuracyPercentageDesc(session.getId());

            if (participations.size() < 2) {
                // Refund all participants
                for (SessionParticipation p : participations) {
                    userService.refundUser(p.getUser().getId(), p.getAmountPaid());
                    p.setStatus(ParticipationStatus.REFUNDED);
                    participationRepository.save(p);
                }

                session.setStatus(CompetitionSessionStatus.CANCELLED);
                sessionRepository.save(session);

                System.out.println("💰 Refunded and cancelled 1v1 session: " + session.getId());
            }
        }
    }
}