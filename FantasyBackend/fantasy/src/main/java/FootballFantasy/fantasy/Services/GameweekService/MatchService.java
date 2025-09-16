package FootballFantasy.fantasy.Services.GameweekService;

import FootballFantasy.fantasy.Entities.GameweekEntities.GameWeek;
import FootballFantasy.fantasy.Entities.GameweekEntities.Match;
import FootballFantasy.fantasy.Entities.GameweekEntities.MatchStatus;
import FootballFantasy.fantasy.Events.MatchCompletedEvent;
import FootballFantasy.fantasy.Repositories.GameweekRepositories.GameWeekRepository;
import FootballFantasy.fantasy.Repositories.GameweekRepositories.MatchRepository;
import FootballFantasy.fantasy.Services.DataService.MatchUpdateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
    @Autowired
    private MatchUpdateService matchUpdateService;
    @Autowired
    private PredictionService predictionService;


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
        existing.setHomeTeam(updatedMatch.getHomeTeam());
        existing.setAwayTeam(updatedMatch.getAwayTeam());
        existing.setMatchDate(updatedMatch.getMatchDate());
        existing.setHomeScore(updatedMatch.getHomeScore());
        existing.setAwayScore(updatedMatch.getAwayScore());
        existing.setDescription(updatedMatch.getDescription());
        existing.setStatus(updatedMatch.getStatus());
        if (updatedMatch.getStatus() == MatchStatus.SCHEDULED) {
            existing.setFinished(false);
        } else if (updatedMatch.getStatus() == MatchStatus.COMPLETED) {
            existing.setFinished(true);
        } else {
            existing.setFinished(updatedMatch.isFinished());
        }
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
            try {
                // ✅ update predictions/accuracy for everyone who picked this match
                predictionService.scorePredictionsForMatch(saved.getId());

                // (optional) still publish your event
                MatchCompletedEvent event = new MatchCompletedEvent(this, matchId);
                eventPublisher.publishEvent(event);

                System.out.println("✅ Predictions scored & event published");
            } catch (Exception e) {
                System.err.println("❌ Error scoring predictions: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("⏭️ No event published - status didn't change to COMPLETED");
        }

        // ✅ If match was reverted from COMPLETED → not COMPLETED, reevaluate gameweek status
        if (oldStatus == MatchStatus.COMPLETED && saved.getStatus() != MatchStatus.COMPLETED) {
            for (GameWeek gameWeek : saved.getGameweeks()) {
                System.out.println("🔁 Match reverted to non-COMPLETED. Reevaluating Gameweek " + gameWeek.getId());
                gameWeekService.updateStatusIfRescheduled(gameWeek.getId());
            }
        }
        for (GameWeek gw : saved.getGameweeks()) {
            gameWeekService.updateStatusIfComplete(gw.getId());
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
        System.out.println("🔄 Setting match " + matchId + " active status to: " + active);

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found with ID: " + matchId));

        boolean wasActive = match.isActive();

        // If no change needed, return early
        if (wasActive == active) {
            System.out.println("✅ No change needed for match " + matchId + " (already " + active + ")");
            return match;
        }

        System.out.println("📊 Match " + matchId + " status changing from " + wasActive + " to " + active);

        // Update the match status
        match.setActive(active);
        Match savedMatch = matchRepository.saveAndFlush(match);

        System.out.println("💾 Match " + matchId + " saved with active=" + savedMatch.isActive());

        // Store affected gameweeks for batch processing
        List<GameWeek> affectedGameweeks = new ArrayList<>(match.getGameweeks());

        // If deactivating a match, remove it from tiebreakers first
        if (!active) {
            System.out.println("🚫 Removing match " + matchId + " from tiebreakers (if any)");
            for (GameWeek gw : affectedGameweeks) {
                removeMatchFromTiebreakers(gw.getId(), matchId);
            }
        }

        // Process each affected gameweek
        for (GameWeek gw : affectedGameweeks) {
            System.out.println("🔄 Processing gameweek " + gw.getId() + " after match status change");

            try {
                // 1) Recalculate dates first (based on active matches only)
                gameWeekService.recalculateGameWeekDates(gw);

                // 2) Update gameweek status (based on active matches only)
                gameWeekService.updateStatusIfRescheduled(gw.getId());

                System.out.println("✅ Gameweek " + gw.getId() + " processed successfully");

            } catch (Exception e) {
                System.err.println("❌ Error processing gameweek " + gw.getId() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        return savedMatch;
    }
    private void removeMatchFromTiebreakers(Long gameweekId, Long matchId) {
        try {
            GameWeek gameWeek = gameWeekRepository.findById(gameweekId)
                    .orElseThrow(() -> new IllegalArgumentException("GameWeek not found"));

            List<Long> currentTiebreakers = gameWeek.getTiebreakerMatchIdList();

            if (currentTiebreakers.contains(matchId)) {
                List<Long> updatedTiebreakers = new ArrayList<>(currentTiebreakers);
                updatedTiebreakers.remove(matchId);

                gameWeek.setTiebreakerMatchIdList(updatedTiebreakers);
                // Update validation status based on remaining tiebreakers
                gameWeek.setValidated(updatedTiebreakers.size() == 3);

                gameWeekRepository.save(gameWeek);

                System.out.println("🗑️ Removed match " + matchId + " from tiebreakers in gameweek " + gameweekId);
            }

        } catch (Exception e) {
            System.err.println("❌ Error removing match from tiebreakers: " + e.getMessage());
        }
    }

}