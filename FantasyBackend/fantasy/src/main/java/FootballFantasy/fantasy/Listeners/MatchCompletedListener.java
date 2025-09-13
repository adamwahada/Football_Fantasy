package FootballFantasy.fantasy.Listeners;

import FootballFantasy.fantasy.Events.MatchCompletedEvent;
import FootballFantasy.fantasy.Events.MatchRescheduledEvent;
import FootballFantasy.fantasy.Services.GameweekService.GameWeekService;
import FootballFantasy.fantasy.Repositories.GameweekRepositories.MatchRepository;
import FootballFantasy.fantasy.Entities.GameweekEntities.Match;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class MatchCompletedListener {

    @Autowired
    private GameWeekService gameWeekService;

    @Autowired
    private MatchRepository matchRepository;

    // Use ONLY TransactionalEventListener to avoid duplicate processing
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMatchCompleted(MatchCompletedEvent event) {
        System.out.println("🎯 MATCH COMPLETED EVENT: Match ID " + event.getMatchId());

        try {
            Match match = matchRepository.findById(event.getMatchId())
                    .orElseThrow(() -> new IllegalArgumentException("Match not found"));

            System.out.println("✅ Processing completed match: " + match.getHomeTeam() + " vs " + match.getAwayTeam());
            System.out.println("📊 Match is part of " + match.getGameweeks().size() + " gameweek(s)");

            // Update status for all gameweeks containing this match
            match.getGameweeks().forEach(gameWeek -> {
                System.out.println("🔄 Checking gameweek " + gameWeek.getId() + " (current status: " + gameWeek.getStatus() + ")");
                boolean updated = gameWeekService.updateStatusIfComplete(gameWeek.getId());

                if (updated) {
                    System.out.println("✅ Gameweek " + gameWeek.getId() + " status updated to FINISHED");
                } else {
                    System.out.println("⏳ Gameweek " + gameWeek.getId() + " not ready to finish yet");
                }
            });
        } catch (Exception e) {
            System.err.println("❌ Error processing match completion: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMatchRescheduled(MatchRescheduledEvent event) {
        System.out.println("🔁 MATCH RESCHEDULED EVENT: Match ID " + event.getMatchId());

        try {
            Match match = matchRepository.findById(event.getMatchId())
                    .orElseThrow(() -> new IllegalArgumentException("Match not found"));

            System.out.println("🔄 Processing rescheduled match: " + match.getHomeTeam() + " vs " + match.getAwayTeam());
            System.out.println("📊 Match is part of " + match.getGameweeks().size() + " gameweek(s)");

            // Update status for all gameweeks containing this match
            match.getGameweeks().forEach(gameWeek -> {
                System.out.println("🔄 Rechecking gameweek " + gameWeek.getId() + " after rescheduling");
                boolean changed = gameWeekService.updateStatusIfComplete(gameWeek.getId());  // Use updateStatusIfComplete instead

                if (changed) {
                    System.out.println("✅ Gameweek " + gameWeek.getId() + " status updated after rescheduling");
                } else {
                    System.out.println("➡️ Gameweek " + gameWeek.getId() + " status unchanged after rescheduling");
                }
            });
        } catch (Exception e) {
            System.err.println("❌ Error processing match rescheduling: " + e.getMessage());
            e.printStackTrace();
        }
    }

}