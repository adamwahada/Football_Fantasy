package FootballFantasy.fantasy.Services.DataService;

import FootballFantasy.fantasy.Dto.MatchResultDTO;
import FootballFantasy.fantasy.Dto.MatchSeedDTO;
import FootballFantasy.fantasy.Entities.GameweekEntity.*;
import FootballFantasy.fantasy.Repositories.GameweekRepository.CompetitionSessionRepository;
import FootballFantasy.fantasy.Repositories.GameweekRepository.GameWeekRepository;
import FootballFantasy.fantasy.Repositories.GameweekRepository.MatchRepository;
import FootballFantasy.fantasy.Services.GameweekService.CompetitionSessionService;
import FootballFantasy.fantasy.Services.GameweekService.GameWeekService;
import FootballFantasy.fantasy.Services.GameweekService.MatchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class MatchSeederService {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private GameWeekRepository gameWeekRepository;

    @Autowired
    private CompetitionSessionService competitionSessionService;

    @Autowired
    private MatchService matchService;

    @Autowired
    private GameWeekService gameWeekService;

    /**
     * Seed matches initially from JSON (scores = 0, not finished)
     */
    @Transactional
    public void seedGameWeekByNumberAndLeague(int weekNumber, LeagueTheme league) throws IOException {
        GameWeek gameWeek = gameWeekRepository.findByWeekNumberAndCompetition(weekNumber, league)
                .orElseThrow(() -> new IllegalArgumentException("GameWeek not found for " + league + " week " + weekNumber));

        if (!gameWeek.getMatches().isEmpty()) {
            throw new IllegalStateException("GameWeek " + weekNumber + " for " + league + " already has matches");
        }

        String fileName = "data/" + league.name().toLowerCase().replace("_", "-") + "-gw" + weekNumber + ".json";
        InputStream inputStream = new ClassPathResource(fileName).getInputStream();
        List<MatchSeedDTO> seedMatches = Arrays.asList(objectMapper.readValue(inputStream, MatchSeedDTO[].class));

        for (MatchSeedDTO dto : seedMatches) {
            Match match = new Match();
            match.setHomeTeam(dto.getHomeTeam());
            match.setAwayTeam(dto.getAwayTeam());
            match.setMatchDate(dto.getMatchDate());
            match.setHomeScore(0);
            match.setAwayScore(0);
            match.setFinished(false);
            match.setStatus(MatchStatus.SCHEDULED);
            match.setPredictionDeadline(dto.getMatchDate().minusMinutes(30));
            match.getGameweeks().add(gameWeek);
            matchRepository.save(match);
            gameWeek.getMatches().add(match);
        }

        updateGameWeekTimings(gameWeek);
    }

    /**
     * Update match results after the gameweek is over using a JSON file
     * 🔥 FIXED: Now properly triggers events by updating each match individually
     */
    @Transactional
    public void updateGameWeekResults(int weekNumber, LeagueTheme league) throws IOException {
        GameWeek gw = gameWeekRepository.findByWeekNumberAndCompetition(weekNumber, league)
                .orElseThrow(() -> new IllegalArgumentException("GameWeek not found"));

        String file = String.format("data/%s-gw%d-results.json",
                league.name().toLowerCase().replace("_","-"), weekNumber);
        InputStream is = new ClassPathResource(file).getInputStream();

        MatchResultDTO[] results = objectMapper.readValue(is, MatchResultDTO[].class);

        int updatedCount = 0;
        int skippedCount = 0;

        for (MatchResultDTO dto : results) {
            // Find matching existing match
            Optional<Match> optionalMatch = gw.getMatches().stream()
                    .filter(m -> m.getHomeTeam().equalsIgnoreCase(dto.getHomeTeam())
                            && m.getAwayTeam().equalsIgnoreCase(dto.getAwayTeam())
                            && m.getMatchDate().isEqual(dto.getMatchDate()))
                    .findFirst();

            if (optionalMatch.isPresent()) {
                Match match = optionalMatch.get();
                MatchStatus oldStatus = match.getStatus();

                // 🔥 KEY FIX: Create a fresh match object with updates
                // This ensures the service layer detects the status change properly
                Match matchUpdate = new Match();
                matchUpdate.setHomeTeam(match.getHomeTeam());
                matchUpdate.setAwayTeam(match.getAwayTeam());
                matchUpdate.setMatchDate(match.getMatchDate());
                matchUpdate.setHomeScore(dto.getHomeScore());
                matchUpdate.setAwayScore(dto.getAwayScore());
                matchUpdate.setStatus(MatchStatus.COMPLETED);
                matchUpdate.setFinished(true);
                matchUpdate.setDescription(match.getDescription());
                matchUpdate.setPredictionDeadline(match.getPredictionDeadline());

                try {
                    matchService.updateMatch(match.getId(), matchUpdate);

                    updatedCount++;
                    System.out.println("✅ Updated: " + dto.getHomeTeam() + " vs " + dto.getAwayTeam() +
                            " with score " + dto.getHomeScore() + "-" + dto.getAwayScore());

                    // Log if this should trigger an event
                    if (oldStatus != MatchStatus.COMPLETED) {
                        System.out.println("🔥 Match completion event should trigger for: " + dto.getHomeTeam() + " vs " + dto.getAwayTeam());
                    }
                } catch (Exception e) {
                    System.err.println("❌ Error updating match " + dto.getHomeTeam() + " vs " + dto.getAwayTeam() + ": " + e.getMessage());
                    skippedCount++;
                }
            } else {
                skippedCount++;
                System.out.println("❌ Skipped (not found): " + dto.getHomeTeam() + " vs " + dto.getAwayTeam() +
                        " on " + dto.getMatchDate());
            }
        }

        System.out.println("\n📊 Update summary: " + updatedCount + " matches updated, " + skippedCount + " matches skipped");

        // 🔥 ADDITIONAL FIX: Explicitly check gameweek status after all matches are updated
        // This is a safety net in case the automatic update doesn't work
        System.out.println("🧪 MANUAL CHECK: Verifying gameweek " + weekNumber + " status after updates");
        gameWeekService.updateStatusIfComplete(gw.getId());
    }

    private void updateGameWeekTimings(GameWeek gameWeek) {
        List<Match> allMatches = gameWeek.getMatches();

        if (allMatches.isEmpty()) return;

        gameWeek.setStartDate(allMatches.stream().map(Match::getMatchDate).min(LocalDateTime::compareTo).orElseThrow());
        gameWeek.setEndDate(allMatches.stream().map(Match::getMatchDate).max(LocalDateTime::compareTo).orElseThrow().plusHours(2).plusMinutes(30));
        gameWeek.setJoinDeadline(gameWeek.getStartDate().minusMinutes(30));

        gameWeekRepository.save(gameWeek);
    }
}