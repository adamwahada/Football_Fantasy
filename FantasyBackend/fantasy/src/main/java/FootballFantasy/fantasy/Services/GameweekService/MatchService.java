package FootballFantasy.fantasy.Services.GameweekService;

import FootballFantasy.fantasy.Entities.GameweekEntity.GameWeek;
import FootballFantasy.fantasy.Entities.GameweekEntity.Match;
import FootballFantasy.fantasy.Entities.GameweekEntity.MatchStatus;
import FootballFantasy.fantasy.Events.MatchCompletedEvent;
import FootballFantasy.fantasy.Repositories.GameweekRepository.GameWeekRepository;
import FootballFantasy.fantasy.Repositories.GameweekRepository.MatchRepository;
import FootballFantasy.fantasy.Services.DataService.TeamIconService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class MatchService {

    @Autowired
    private MatchRepository matchRepository;
    @Autowired
    private GameWeekRepository gameWeekRepository;
    @Autowired
    private GameWeekService gameWeekService;
    @Autowired
    private CompetitionSessionService competitionSessionService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Match createMatch(Match match) {
        // ✅ 1. Check for duplicate BEFORE any other logic
        Optional<Match> existingMatch = matchRepository.findDuplicateMatch(
                match.getHomeTeam(),
                match.getAwayTeam(),
                match.getMatchDate()
        );

        if (existingMatch.isPresent()) {
            throw new RuntimeException("Un match entre " + match.getHomeTeam() +
                    " et " + match.getAwayTeam() + " le " +
                    match.getMatchDate().toLocalDate() + " existe déjà.");
        }

        // ✅ 2. Validate gameweeks (if present)
        if (match.getGameweeks() != null) {
            for (GameWeek gw : match.getGameweeks()) {
                GameWeek gameWeek = gameWeekRepository.findById(gw.getId())
                        .orElseThrow(() -> new IllegalArgumentException("GameWeek not found with ID: " + gw.getId()));

                // Ensure match date is within gameweek boundaries
                if (match.getMatchDate().isBefore(gameWeek.getStartDate()) ||
                        match.getMatchDate().isAfter(gameWeek.getEndDate())) {
                    throw new IllegalArgumentException("Match date is outside the boundaries of GameWeek '");
                }

                // Ensure bidirectional relationship
                if (!gameWeek.getMatches().contains(match)) {
                    gameWeek.getMatches().add(match);
                }
            }
        }

        // ✅ 3. Set prediction deadline
        match.setPredictionDeadline(match.getMatchDate().minusMinutes(30));

        // ✅ 4. Save and return
        return matchRepository.save(match);
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Match updateMatch(Long matchId, Match updatedMatch) {
        System.out.println("🔄 MatchService.updateMatch called for match ID: " + matchId);

        Match existing = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found"));

        // Store the old status to check if it changed
        MatchStatus oldStatus = existing.getStatus();
        System.out.println("📊 Match: " + existing.getHomeTeam() + " vs " + existing.getAwayTeam());
        System.out.println("📊 Current match status: " + oldStatus);
        System.out.println("📊 New match status: " + updatedMatch.getStatus());
        System.out.println("📊 New scores: " + updatedMatch.getHomeScore() + "-" + updatedMatch.getAwayScore());

        existing.setHomeTeam(updatedMatch.getHomeTeam());
        existing.setAwayTeam(updatedMatch.getAwayTeam());
        existing.setMatchDate(updatedMatch.getMatchDate());
        existing.setHomeScore(updatedMatch.getHomeScore());
        existing.setAwayScore(updatedMatch.getAwayScore());
        existing.setFinished(updatedMatch.isFinished());
        existing.setDescription(updatedMatch.getDescription());
        existing.setStatus(updatedMatch.getStatus());

        Match saved = matchRepository.saveAndFlush(existing);
        System.out.println("💾 Match saved successfully");
        System.out.println("💾 Final saved status: " + saved.getStatus());

        // Check if we should publish event
        boolean shouldPublishEvent = (oldStatus != MatchStatus.COMPLETED && saved.getStatus() == MatchStatus.COMPLETED);
        System.out.println("🔍 Should publish event? " + shouldPublishEvent);
        System.out.println("🔍 Old status != COMPLETED: " + (oldStatus != MatchStatus.COMPLETED));
        System.out.println("🔍 New status == COMPLETED: " + (saved.getStatus() == MatchStatus.COMPLETED));

        // Publish event if match was just completed
        if (shouldPublishEvent) {
            System.out.println("🚀 Publishing MatchCompletedEvent for match ID: " + matchId);

            // Check if we're in a transaction
            boolean inTransaction = org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive();
            System.out.println("📝 Transaction active: " + inTransaction);

            try {
                MatchCompletedEvent event = new MatchCompletedEvent(this, matchId);
                eventPublisher.publishEvent(event);
                System.out.println("✅ MatchCompletedEvent published successfully");
            } catch (Exception e) {
                System.err.println("❌ Error publishing event: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("⏭️ No event published - status didn't change to COMPLETED");
        }

        return saved;
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional
    public void deleteMatch(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found"));
        for (GameWeek gw : match.getGameweeks()) {
            gw.getMatches().remove(match);
            gameWeekService.recalculateGameWeekDates(gw);
        }
        matchRepository.delete(match);
    }

    public Match getMatchById(Long matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found"));
    }

    public List<Match> getAllMatches() {
        return matchRepository.findAll();
    }

    public String getWinner(Long matchId) {
        Match match = getMatchById(matchId);
        if (!match.isFinished()) return "Match not finished yet.";
        int home = match.getHomeScore() != null ? match.getHomeScore() : 0;
        int away = match.getAwayScore() != null ? match.getAwayScore() : 0;
        return home > away ? match.getHomeTeam()
                : away > home ? match.getAwayTeam()
                : "Draw";
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Transactional
    public Match setMatchActiveStatus(Long matchId, boolean active) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found with ID: " + matchId));
        match.setActive(active);
        return matchRepository.save(match);
    }
}