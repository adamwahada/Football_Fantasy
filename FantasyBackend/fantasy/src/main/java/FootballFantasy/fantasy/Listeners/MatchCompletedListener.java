package FootballFantasy.fantasy.Listeners;

import FootballFantasy.fantasy.Events.MatchCompletedEvent;
import FootballFantasy.fantasy.Services.GameweekService.GameWeekService;
import FootballFantasy.fantasy.Repositories.GameweekRepository.MatchRepository;
import FootballFantasy.fantasy.Entities.GameweekEntity.Match;
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

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMatchCompleted(MatchCompletedEvent event) {
        // Get the match and its associated gameweeks
        Match match = matchRepository.findById(event.getMatchId())
                .orElseThrow(() -> new IllegalArgumentException("Match not found"));

        // Log the match completion
        System.out.println("Match ID " + event.getMatchId() + " completed.");

        // Update status for all gameweeks containing this match
        match.getGameweeks().forEach(gameWeek -> {
            if (gameWeekService.updateStatusIfComplete(gameWeek.getId())) {
                // Log the gameweek status update
                System.out.println("Gameweek " + gameWeek.getId() + " status updated to FINISHED.");
            }
        });
    }
}
