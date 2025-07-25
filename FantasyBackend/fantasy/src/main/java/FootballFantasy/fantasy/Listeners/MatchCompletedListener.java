package FootballFantasy.fantasy.Listeners;

import FootballFantasy.fantasy.Events.MatchCompletedEvent;
import FootballFantasy.fantasy.Repositories.GameweekRepository.PredictionRepository;
import FootballFantasy.fantasy.Services.GameweekService.GameWeekService;
import FootballFantasy.fantasy.Repositories.GameweekRepository.MatchRepository;
import FootballFantasy.fantasy.Entities.GameweekEntity.Match;
import FootballFantasy.fantasy.Services.GameweekService.PredictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class MatchCompletedListener {

    @Autowired
    private GameWeekService gameWeekService;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private PredictionService predictionService;

    // 🔥 OPTION 1: Use TransactionalEventListener to run after transaction commits
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMatchCompletedAfterCommit(MatchCompletedEvent event) {
        System.out.println("🎯 AFTER_COMMIT EVENT LISTENER TRIGGERED: Match ID " + event.getMatchId());

        Match match = matchRepository.findById(event.getMatchId())
                .orElseThrow(() -> new IllegalArgumentException("Match not found"));

        match.getGameweeks().forEach(gameWeek -> {
            boolean updated = gameWeekService.updateStatusIfComplete(gameWeek.getId());

            if (updated) {
                System.out.println("✅ Gameweek " + gameWeek.getId() + " status updated to FINISHED");

                // ✅ Trigger finalization
                predictionService.finalizeGameweekAfterCompletion(gameWeek.getId());
            }
        });
    }

    // 🔥 OPTION 2: Fallback regular EventListener with proper transaction handling
    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleMatchCompletedFallback(MatchCompletedEvent event) {
        System.out.println("🎯 FALLBACK EVENT LISTENER TRIGGERED: Match ID " + event.getMatchId() + " completed");
        System.out.println("🎯 Thread: " + Thread.currentThread().getName());

        try {
            // Check if we're in a transaction
            boolean inTransaction = org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive();
            System.out.println("📝 Transaction active in listener: " + inTransaction);

            // Get the match and its associated gameweeks
            Match match = matchRepository.findById(event.getMatchId())
                    .orElseThrow(() -> new IllegalArgumentException("Match not found"));

            System.out.println("🔍 Processing match: " + match.getHomeTeam() + " vs " + match.getAwayTeam());
            System.out.println("📊 Match is part of " + match.getGameweeks().size() + " gameweek(s)");

            // Update status for all gameweeks containing this match
            match.getGameweeks().forEach(gameWeek -> {
                System.out.println("🔄 Checking gameweek " + gameWeek.getId() + " (current status: " + gameWeek.getStatus() + ")");
                boolean updated = gameWeekService.updateStatusIfComplete(gameWeek.getId());

                if (updated) {
                    System.out.println("✅ Gameweek " + gameWeek.getId() + " status updated to FINISHED");
                } else {
                    System.out.println("⏳ Gameweek " + gameWeek.getId() + " is not complete yet or was already finished");
                }
            });
        } catch (Exception e) {
            System.err.println("❌ Error in fallback match completion handler: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("🎯 FALLBACK EVENT LISTENER COMPLETED for match ID " + event.getMatchId());
    }
}