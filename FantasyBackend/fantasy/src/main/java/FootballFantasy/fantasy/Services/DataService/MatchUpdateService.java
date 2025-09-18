package FootballFantasy.fantasy.Services.DataService;

import FootballFantasy.fantasy.Entities.GameweekEntities.*;
import FootballFantasy.fantasy.Repositories.GameweekRepositories.GameWeekRepository;
import FootballFantasy.fantasy.Repositories.GameweekRepositories.MatchRepository;
import FootballFantasy.fantasy.Services.GameweekService.CompetitionSessionService;
import FootballFantasy.fantasy.Services.GameweekService.PredictionService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MatchUpdateService {

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private GameWeekRepository gameweekRepository;

    @Autowired
    private PredictionService predictionService;

    @Autowired
    private CompetitionSessionService competitionSessionService;

    @Value("${football.api.key}")
    private String apiKey;

    @Value("${football.api.url-template}")
    private String apiUrlTemplate;

    // ✅ Automatic update every 2 hours
    @Scheduled(cron = "0 0 */2 * * *")
    public void updateMatchesAutomatically() {
        System.out.println("⏰ AUTOMATIC UPDATE: Updating today's matches...");
        updateTodaysMatches();
        System.out.println("⏰ AUTOMATIC UPDATE: Completed");
    }

    // ✅ Simplified method for today's matches - removes complex timing checks
    @Transactional
    public void updateTodaysMatches() {
        System.out.println("📅 Starting update for TODAY's matches across all competitions...");

        LocalDate today = LocalDate.now();
        System.out.println("📅 Today's date: " + today);

        int totalUpdated = 0;

        for (LeagueTheme league : LeagueTheme.values()) {
            try {
                if (!league.isApiAvailable()) {
                    continue;
                }

                // Find which gameweeks have matches today for this league
                Set<Integer> gameweeksWithTodaysMatches = findGameweeksWithMatchesToday(league, today);

                if (gameweeksWithTodaysMatches.isEmpty()) {
                    System.out.println("📅 No matches today for " + league);
                    continue;
                }

                System.out.println("📅 " + league + " has matches today in gameweeks: " + gameweeksWithTodaysMatches);

                // Update each gameweek that has matches today
                for (Integer weekNumber : gameweeksWithTodaysMatches) {
                    try {
                        System.out.println("🔄 Updating " + league + " gameweek " + weekNumber);
                        updateMatchesForGameweek(league.name(), weekNumber);
                        totalUpdated++;
                    } catch (Exception e) {
                        System.out.println("❌ Error updating " + league + " week " + weekNumber + ": " + e.getMessage());
                    }
                }

            } catch (Exception e) {
                System.out.println("❌ Error checking " + league + ": " + e.getMessage());
            }
        }

        System.out.println("📊 TODAY's matches update complete - Updated " + totalUpdated + " gameweeks");
    }

    // ✅ Helper method to find which gameweeks have matches today
    private Set<Integer> findGameweeksWithMatchesToday(LeagueTheme league, LocalDate targetDate) {
        try {
            // Fetch all matches from API (same as your existing method)
            String apiUrl = apiUrlTemplate
                    .replace("{competition}", league.getApiCode())
                    .replace("{season}", "2025");

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Auth-Token", apiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(apiUrl, HttpMethod.GET, entity, Map.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                System.out.println("⚠️ API call failed for " + league);
                return new HashSet<>();
            }

            List<Map<String, Object>> matches = (List<Map<String, Object>>) response.getBody().get("matches");
            if (matches == null || matches.isEmpty()) {
                return new HashSet<>();
            }

            // Find gameweeks that have matches today
            Set<Integer> gameweeksWithTodaysMatches = new HashSet<>();

            for (Map<String, Object> match : matches) {
                String dateStr = (String) match.get("utcDate");
                Integer matchday = (Integer) match.get("matchday");

                if (dateStr == null || matchday == null) continue;

                LocalDate matchDate = OffsetDateTime.parse(dateStr).toLocalDate();

                if (matchDate.equals(targetDate)) {
                    gameweeksWithTodaysMatches.add(matchday);
                }
            }

            return gameweeksWithTodaysMatches;

        } catch (Exception e) {
            System.out.println("❌ Error finding today's gameweeks for " + league + ": " + e.getMessage());
            return new HashSet<>();
        }
    }

    // ✅ New streamlined method that processes matches for a specific gameweek without timing restrictions
// ✅ New streamlined method that processes matches for a specific gameweek
    @Transactional
    public void updateMatchesForGameweek(String competition, int weekNumber) {
        try {
            System.out.println("🔍 Updating competition: '" + competition + "', weekNumber: " + weekNumber);

            LeagueTheme league = mapToLeagueTheme(competition);
            if (league == null) {
                System.out.println("❌ Invalid competition: " + competition);
                throw new IllegalArgumentException("Invalid competition: " + competition);
            }

            String apiUrl = apiUrlTemplate
                    .replace("{competition}", league.getApiCode())
                    .replace("{season}", "2025");

            System.out.println("🔍 API URL: " + apiUrl);

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Auth-Token", apiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(apiUrl, HttpMethod.GET, entity, Map.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("API call failed for " + league + " with status: " + response.getStatusCode());
            }

            List<Map<String, Object>> matches = (List<Map<String, Object>>) response.getBody().get("matches");
            if (matches == null || matches.isEmpty()) {
                System.out.println("⚠️ No matches found in API response");
                return;
            }

            // Filter only matches from the requested week
            List<Map<String, Object>> filteredMatches = matches.stream()
                    .filter(m -> Objects.equals((Integer) m.get("matchday"), weekNumber))
                    .toList();

            System.out.println("➡️ " + filteredMatches.size() + " matches found for " + league + " week " + weekNumber);

            if (filteredMatches.isEmpty()) {
                System.out.println("⚠️ No matches found for week " + weekNumber + " in " + league);
                return;
            }

            Set<GameWeek> affectedGameWeeks = new HashSet<>();

            for (Map<String, Object> matchData : filteredMatches) {
                try {
                    String status = (String) matchData.get("status");
                    String utcDateStr = (String) matchData.get("utcDate");

                    if (utcDateStr == null) {
                        continue;
                    }

                    Instant matchDate = Instant.parse(utcDateStr);
                    boolean isFuture = matchDate.isAfter(Instant.now());
                    boolean isFinishedOrLive = "FINISHED".equals(status) || "IN_PLAY".equals(status) || "PAUSED".equals(status);

                    // ⏭️ Skip future scheduled matches
                    if (isFuture && ("SCHEDULED".equals(status) || "TIMED".equals(status))) {
                        System.out.println("⏭️ Skipping future match: " + matchData.get("homeTeam"));
                        continue;
                    }

                    // ✅ Process only finished/live or past scheduled ones
                    if (isFinishedOrLive || !isFuture) {
                        processMatchDataSimplified(matchData, league, affectedGameWeeks);
                    }

                } catch (Exception e) {
                    System.out.println("⚠️ Error processing match: " + e.getMessage());
                }
            }

            // Update GameWeek timings & statuses
            for (GameWeek gw : affectedGameWeeks) {
                updateGameWeekTimings(gw);
            }
            updateGameweeks(affectedGameWeeks);

            System.out.println("✅ Successfully updated " + affectedGameWeeks.size() + " gameweek(s)");

        } catch (Exception e) {
            System.out.println("❌ Error updating " + competition + " week " + weekNumber + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to update matches for " + competition + " week " + weekNumber, e);
        }
    }


// ✅ Simplified match processing without overriding inactive matches
@Transactional
protected void processMatchDataSimplified(Map<String, Object> matchData, LeagueTheme league, Set<GameWeek> affectedGameWeeks) {
    try {
        Map<String, Object> homeTeamMap = (Map<String, Object>) matchData.get("homeTeam");
        Map<String, Object> awayTeamMap = (Map<String, Object>) matchData.get("awayTeam");

        if (homeTeamMap == null || awayTeamMap == null) {
            System.out.println("⚠️ Missing team data in match");
            return;
        }

        String homeTeamName = normalizeTeamName((String) homeTeamMap.get("name"));
        String awayTeamName = normalizeTeamName((String) awayTeamMap.get("name"));
        String apiStatus = (String) matchData.get("status");

        String dateStr = (String) matchData.get("utcDate");
        if (dateStr == null) {
            System.out.println("⚠️ No date found for match: " + homeTeamName + " vs " + awayTeamName);
            return;
        }

        LocalDateTime matchDateUtc = OffsetDateTime.parse(dateStr).toLocalDateTime();
        Integer matchday = (Integer) matchData.get("matchday");

        if (matchday == null) {
            System.out.println("⚠️ No matchday found for " + homeTeamName + " vs " + awayTeamName);
            return;
        }

        // Find or create GameWeek
        GameWeek gameWeek = gameweekRepository.findByWeekNumberAndCompetition(matchday, league)
                .orElse(null);

        if (gameWeek == null) {
            System.out.println("🆕 Creating new GameWeek for " + league + " week " + matchday);
            gameWeek = new GameWeek();
            gameWeek.setWeekNumber(matchday);
            gameWeek.setCompetition(league);
            gameWeek.setStatus(GameweekStatus.UPCOMING);
            gameWeek.setStartDate(matchDateUtc.minusDays(3));
            gameWeek.setEndDate(matchDateUtc.plusDays(3));
            gameWeek.setJoinDeadline(matchDateUtc.minusHours(2));
            gameWeek = gameweekRepository.save(gameWeek);
        }

        // Parse score
        Integer homeGoals = null;
        Integer awayGoals = null;
        Map<String, Object> scoreMap = (Map<String, Object>) matchData.get("score");
        if (scoreMap != null) {
            Map<String, Object> fullTime = (Map<String, Object>) scoreMap.get("fullTime");
            if (fullTime != null) {
                homeGoals = fullTime.get("home") != null ? (Integer) fullTime.get("home") : null;
                awayGoals = fullTime.get("away") != null ? (Integer) fullTime.get("away") : null;
            }
        }

        // Find existing match
        Match dbMatch = matchRepository.findWithGameweeks(
                homeTeamName, awayTeamName,
                matchDateUtc.minusHours(2), matchDateUtc.plusHours(2)
        );

        // Store previous status to detect changes
        boolean wasFinishedBefore = dbMatch != null && dbMatch.isFinished();

        // Respect inactive flag
        if (dbMatch != null && !dbMatch.isActive()) {
            System.out.println("⏭️ Skipping inactive match: " + homeTeamName + " vs " + awayTeamName);
            return;
        }

        if (dbMatch == null) {
            System.out.println("🆕 Creating new match: " + homeTeamName + " vs " + awayTeamName);
            dbMatch = new Match();
            dbMatch.setHomeTeam(homeTeamName);
            dbMatch.setAwayTeam(awayTeamName);
            dbMatch.setGameweeks(new ArrayList<>());
            dbMatch.setActive(true);
        } else {
            System.out.println("🔄 Updating existing match: " + homeTeamName + " vs " + awayTeamName);
        }

        // Update base info
        dbMatch.setMatchDate(matchDateUtc);
        dbMatch.setPredictionDeadline(matchDateUtc.minusMinutes(30));

        // Map API status properly
        boolean justFinished = false;
        switch (apiStatus) {
            case "FINISHED" -> {
                if (homeGoals != null && awayGoals != null) {
                    dbMatch.setHomeScore(homeGoals);
                    dbMatch.setAwayScore(awayGoals);
                }
                dbMatch.setFinished(true);
                dbMatch.setStatus(MatchStatus.COMPLETED);
                justFinished = !wasFinishedBefore;
            }
            case "IN_PLAY", "PAUSED" -> {
                if (homeGoals != null && awayGoals != null) {
                    dbMatch.setHomeScore(homeGoals);
                    dbMatch.setAwayScore(awayGoals);
                }
                dbMatch.setFinished(false);
                dbMatch.setStatus(MatchStatus.LIVE);
            }
            case "SCHEDULED", "TIMED", "POSTPONED" -> {
                dbMatch.setHomeScore(null);
                dbMatch.setAwayScore(null);
                dbMatch.setFinished(false);
                dbMatch.setStatus(MatchStatus.SCHEDULED);
            }
            default -> {
                System.out.println("⚠️ Unknown match status: " + apiStatus);
                dbMatch.setHomeScore(null);
                dbMatch.setAwayScore(null);
                dbMatch.setFinished(false);
                dbMatch.setStatus(MatchStatus.SCHEDULED);
            }
        }

        // Link to GameWeek
        if (!dbMatch.getGameweeks().contains(gameWeek)) {
            dbMatch.getGameweeks().add(gameWeek);
        }

        // Save the match
        dbMatch = matchRepository.save(dbMatch);
        System.out.println("💾 Saved match: " + homeTeamName + " vs " + awayTeamName +
                " (Status: " + dbMatch.getStatus() + ")");

        // IMMEDIATELY SCORE PREDICTIONS IF MATCH JUST FINISHED
        if (justFinished && dbMatch.getHomeScore() != null && dbMatch.getAwayScore() != null) {
            System.out.println("⚡ Match just finished! Scoring predictions immediately...");
            predictionService.scorePredictionsForMatch(dbMatch.getId());
        }

        affectedGameWeeks.add(gameWeek);

    } catch (Exception e) {
        System.out.println("❌ Error processing match data: " + e.getMessage());
        throw e;
    }
}


    // ✅ Keep the old method for backward compatibility (manual finished-only updates)
    @Transactional
    public void updateMatches() {
        System.out.println("🚀 Starting match update for all leagues (FINISHED only)...");
        int successCount = 0;
        int errorCount = 0;

        for (LeagueTheme league : LeagueTheme.values()) {
            try {
                if (!league.isApiAvailable()) {
                    System.out.println("⏭️ Skipping league not available in API: " + league.name());
                    continue;
                }

                System.out.println("➡️ Starting update for competition: " + league.name() + " (FINISHED only)");
                long startTime = System.currentTimeMillis();

                fetchAndUpdateMatches(league, 2025, GameweekStatus.FINISHED);

                long endTime = System.currentTimeMillis();
                System.out.println("✅ Successfully updated FINISHED matches for "
                        + league.name() + " in " + (endTime - startTime) + "ms");
                successCount++;

            } catch (Exception e) {
                System.out.println("❌ Exception while updating " + league.name() + ": " + e.getMessage());
                e.printStackTrace();
                errorCount++;
            }
        }

        System.out.println("📊 Update complete (FINISHED only) - Success: "
                + successCount + ", Errors: " + errorCount);
    }

    @Transactional
    public void updateMatchesManually(String competition) {
        System.out.println("🎯 Manual update triggered for: " + competition + " (FINISHED only)");

        try {
            LeagueTheme league = LeagueTheme.valueOf(competition);
            long startTime = System.currentTimeMillis();

            fetchAndUpdateMatches(league, 2025, GameweekStatus.FINISHED);

            long endTime = System.currentTimeMillis();
            System.out.println("✅ Manual update completed for " + competition
                    + " (FINISHED only) in " + (endTime - startTime) + "ms");

        } catch (IllegalArgumentException e) {
            System.out.println("❌ Invalid competition: " + competition);
            throw new RuntimeException("Invalid competition: " + competition, e);
        } catch (Exception e) {
            System.out.println("❌ Error in manual update for " + competition + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to update matches for " + competition, e);
        }
    }

    // ✅ Dynamic fetch method that updates all matches and calculates gameweek status
    private void fetchAndUpdateMatchesDynamic(LeagueTheme league, int season) {
        try {
            String apiUrl = apiUrlTemplate
                    .replace("{competition}", league.getApiCode())
                    .replace("{season}", String.valueOf(season));

            System.out.println("🔗 API URL: " + apiUrl);

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Auth-Token", apiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(apiUrl, HttpMethod.GET, entity, Map.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("API call failed for " + league + " - Status: " + response.getStatusCode());
            }

            List<Map<String, Object>> matches = (List<Map<String, Object>>) response.getBody().get("matches");
            if (matches == null || matches.isEmpty()) {
                System.out.println("⚠️ No matches found for " + league);
                return;
            }

            System.out.println("📥 " + matches.size() + " matches received from " + league);

            // Filter matches to relevant weeks
            List<Map<String, Object>> filteredMatches = filterMatchesByRelevantWeeks(matches, league);

            // Group matches by gameweek
            Map<Integer, List<Map<String, Object>>> matchesByGameweek =
                    filteredMatches.stream().collect(Collectors.groupingBy(m -> (Integer) m.get("matchday")));

            Set<GameWeek> affectedGameWeeks = new HashSet<>();
            int processedCount = 0;

            for (Map.Entry<Integer, List<Map<String, Object>>> entry : matchesByGameweek.entrySet()) {
                Integer gameweekNumber = entry.getKey();
                List<Map<String, Object>> matchesForWeek = entry.getValue();

                System.out.println("🎯 Processing Gameweek " + gameweekNumber + " (ALL MATCHES)");

                for (Map<String, Object> matchData : matchesForWeek) {
                    try {
                        processMatchData(matchData, league, affectedGameWeeks);
                        processedCount++;
                    } catch (Exception e) {
                        System.out.println("⚠️ Error processing match data: " + e.getMessage());
                    }
                }
            }

            System.out.println("📝 Processed " + processedCount + " matches for " + league);

            // Update gameweek timings and calculate status dynamically
            for (GameWeek gw : affectedGameWeeks) {
                try {
                    updateGameWeekTimings(gw);
                    // Don't set status here - let updateGameweeks calculate it based on matches
                } catch (Exception e) {
                    System.out.println("⚠️ Error updating GameWeek " + gw.getWeekNumber() + ": " + e.getMessage());
                }
            }

            // ✅ Calculate gameweek status based on actual match statuses
            updateGameweeks(affectedGameWeeks);

        } catch (Exception e) {
            System.out.println("❌ Error fetching " + league + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch and update matches for " + league, e);
        }
    }

    // ✅ Filter matches to only include current, past, and next 3 weeks
    private List<Map<String, Object>> filterMatchesByRelevantWeeks(List<Map<String, Object>> matches, LeagueTheme league) {
        // Get current gameweek for this league
        Integer currentWeek = getCurrentGameweek(league);

        if (currentWeek == null) {
            System.out.println("⚠️ Could not determine current week for " + league + ", processing all matches");
            return matches;
        }

        System.out.println("📅 Current week for " + league + ": " + currentWeek);

        // Define the range: all past weeks + current + next 3
        Set<Integer> relevantWeeks = new HashSet<>();

        // Add all weeks from 1 to current week (past + current)
        for (int week = 1; week <= currentWeek; week++) {
            relevantWeeks.add(week);
        }

        // Add next 3 weeks
        for (int week = currentWeek + 1; week <= currentWeek + 3; week++) {
            relevantWeeks.add(week);
        }

        System.out.println("🎯 Updating weeks: " + relevantWeeks.stream().sorted().toList());

        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> validMatches = new ArrayList<>();
        int ignoredAdvancedMatches = 0;
        int ignoredPostponedMatches = 0;

        for (Map<String, Object> match : matches) {
            Integer matchday = (Integer) match.get("matchday");
            if (matchday == null || !relevantWeeks.contains(matchday)) {
                continue; // Skip matches outside our week range
            }

            String dateStr = (String) match.get("utcDate");
            if (dateStr == null) continue;

            LocalDateTime matchDate = OffsetDateTime.parse(dateStr).toLocalDateTime();

            // ✅ Check if match is played at wrong time for prediction game
            if (isMatchPlayedAtWrongTime(matchday, matchDate, currentWeek, now)) {
                Map<String, Object> homeTeam = (Map<String, Object>) match.get("homeTeam");
                Map<String, Object> awayTeam = (Map<String, Object>) match.get("awayTeam");

                if (matchday > currentWeek + 3) {
                    System.out.println("🚫 IGNORED: Future match played too early - GW" + matchday +
                            " " + homeTeam.get("name") + " vs " + awayTeam.get("name") +
                            " (playing on " + matchDate.toLocalDate() + " but we're only at GW" + currentWeek + ")");
                    ignoredAdvancedMatches++;
                } else {
                    System.out.println("🚫 IGNORED: Past match played too late - GW" + matchday +
                            " " + homeTeam.get("name") + " vs " + awayTeam.get("name") +
                            " (should have been before GW" + currentWeek + " but playing on " + matchDate.toLocalDate() + ")");
                    ignoredPostponedMatches++;
                }
                continue; // Skip this match
            }

            validMatches.add(match);
        }

        if (ignoredAdvancedMatches > 0 || ignoredPostponedMatches > 0) {
            System.out.println("📊 " + league + " - Ignored " + ignoredAdvancedMatches +
                    " advanced matches and " + ignoredPostponedMatches + " postponed matches");
        }

        return validMatches;
    }

    // ✅ Check if a match is being played at the wrong time for prediction purposes
    private boolean isMatchPlayedAtWrongTime(int matchday, LocalDateTime matchDate, int currentWeek, LocalDateTime now) {
        LocalDateTime expectedWeekStart = calculateExpectedGameweekStart(matchday);
        LocalDateTime expectedWeekEnd = expectedWeekStart.plusDays(7);

        // Allow some flexibility (±10 days) but catch extreme cases
        LocalDateTime flexibleStart = expectedWeekStart.minusDays(10);
        LocalDateTime flexibleEnd = expectedWeekEnd.plusDays(10);

        // Case 1: Future gameweek match being played too early
        if (matchday > currentWeek + 3 && matchDate.isBefore(flexibleStart)) {
            return true; // This future match is being played way too early
        }

        // Case 2: Past gameweek match being played too late
        if (matchday < currentWeek && matchDate.isAfter(flexibleEnd)) {
            return true; // This past match is being played way too late
        }

        // Case 3: Current/near future match being played way too early
        if (matchday >= currentWeek && matchday <= currentWeek + 3 &&
                matchDate.isBefore(now.minusDays(30))) {
            return true; // Match is from current period but played over 30 days ago
        }

        return false; // Match timing is acceptable
    }

    // ✅ Calculate expected gameweek start based on season pattern and week number
    private LocalDateTime calculateExpectedGameweekStart(int weekNumber) {
        // Season start dates for 2024-2025 season
        LocalDateTime seasonStart = LocalDateTime.of(2024, 8, 17, 15, 0); // Mid August

        // Each gameweek is roughly 7 days apart
        return seasonStart.plusDays((weekNumber - 1) * 7L);
    }

    // ✅ Determine current gameweek based on today's date and existing gameweeks
    private Integer getCurrentGameweek(LeagueTheme league) {
        LocalDateTime now = LocalDateTime.now();

        // Try to find current gameweek based on dates
        List<GameWeek> gameweeks = gameweekRepository.findByCompetitionOrderByWeekNumber(league);

        for (GameWeek gw : gameweeks) {
            if (gw.getStartDate() != null && gw.getEndDate() != null) {
                if ((now.isAfter(gw.getStartDate()) || now.isEqual(gw.getStartDate())) &&
                        (now.isBefore(gw.getEndDate()) || now.isEqual(gw.getEndDate()))) {
                    System.out.println("📅 Current week determined by date range: " + gw.getWeekNumber());
                    return gw.getWeekNumber();
                }
            }
        }

        // Fallback: find the latest gameweek that's not finished
        for (GameWeek gw : gameweeks) {
            if (gw.getStatus() != GameweekStatus.FINISHED) {
                System.out.println("📅 Current week determined by status: " + gw.getWeekNumber());
                return gw.getWeekNumber();
            }
        }

        // Last fallback: use the highest week number we have
        if (!gameweeks.isEmpty()) {
            GameWeek latest = gameweeks.get(gameweeks.size() - 1);
            System.out.println("📅 Current week fallback to latest: " + latest.getWeekNumber());
            return latest.getWeekNumber();
        }

        // Ultimate fallback if no gameweeks exist yet
        System.out.println("📅 No gameweeks found, assuming week 1");
        return 1;
    }

    private void fetchAndUpdateMatches(LeagueTheme league, int season, GameweekStatus status) {
        try {
            String apiUrl = apiUrlTemplate
                    .replace("{competition}", league.getApiCode())
                    .replace("{season}", String.valueOf(season));

            System.out.println("🔗 API URL: " + apiUrl);

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Auth-Token", apiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(apiUrl, HttpMethod.GET, entity, Map.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("API call failed for " + league + " - Status: " + response.getStatusCode());
            }

            List<Map<String, Object>> matches = (List<Map<String, Object>>) response.getBody().get("matches");
            if (matches == null || matches.isEmpty()) {
                System.out.println("⚠️ No matches found for " + league);
                return;
            }

            System.out.println("📥 " + matches.size() + " matches received from " + league);

            // Filter matches to relevant weeks
            List<Map<String, Object>> filteredMatches = filterMatchesByRelevantWeeks(matches, league);

            // Group matches by gameweek
            Map<Integer, List<Map<String, Object>>> matchesByGameweek =
                    filteredMatches.stream().collect(Collectors.groupingBy(m -> (Integer) m.get("matchday")));

            Set<GameWeek> affectedGameWeeks = new HashSet<>();
            int processedCount = 0;

            for (Map.Entry<Integer, List<Map<String, Object>>> entry : matchesByGameweek.entrySet()) {
                Integer gameweekNumber = entry.getKey();
                List<Map<String, Object>> matchesForWeek = entry.getValue();

                // Only process gameweeks matching the requested status
                boolean isFinished = matchesForWeek.stream()
                        .allMatch(m -> "FINISHED".equalsIgnoreCase((String) m.get("status")));

                if (status == GameweekStatus.FINISHED && !isFinished) {
                    System.out.println("⏭️ Skipping Gameweek " + gameweekNumber + " because it's not finished yet.");
                    continue;
                }

                // You can add similar checks for ONGOING or UPCOMING if needed

                System.out.println("🎯 Processing Gameweek " + gameweekNumber + " with status " + status);

                for (Map<String, Object> matchData : matchesForWeek) {
                    try {
                        processMatchData(matchData, league, affectedGameWeeks);
                        processedCount++;
                    } catch (Exception e) {
                        System.out.println("⚠️ Error processing match data: " + e.getMessage());
                    }
                }
            }

            System.out.println("📝 Processed " + processedCount + " matches for " + league + " with status " + status);

            // Update gameweek timings + status
            for (GameWeek gw : affectedGameWeeks) {
                try {
                    updateGameWeekTimings(gw);
                    gw.setStatus(status); // ✅ set the desired GameweekStatus
                    System.out.println("✅ Updated GameWeek " + gw.getWeekNumber() + " with status " + status);
                } catch (Exception e) {
                    System.out.println("⚠️ Error updating GameWeek " + gw.getWeekNumber() + ": " + e.getMessage());
                }
            }

            updateGameweeks(affectedGameWeeks);

        } catch (Exception e) {
            System.out.println("❌ Error fetching " + league + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to fetch and update matches for " + league, e);
        }
    }
    // ✅ Process match data (full version with timing validations)
    @Transactional
    protected void processMatchData(Map<String, Object> matchData, LeagueTheme league, Set<GameWeek> affectedGameWeeks) {
        try {
            Map<String, Object> homeTeamMap = (Map<String, Object>) matchData.get("homeTeam");
            Map<String, Object> awayTeamMap = (Map<String, Object>) matchData.get("awayTeam");

            if (homeTeamMap == null || awayTeamMap == null) {
                System.out.println("⚠️ Missing team data in match");
                return;
            }

            String homeTeamName = normalizeTeamName((String) homeTeamMap.get("name"));
            String awayTeamName = normalizeTeamName((String) awayTeamMap.get("name"));
            String apiStatus = (String) matchData.get("status");

            String dateStr = (String) matchData.get("utcDate");
            if (dateStr == null) {
                System.out.println("⚠️ No date found for match: " + homeTeamName + " vs " + awayTeamName);
                return;
            }

            LocalDateTime matchDateUtc = OffsetDateTime.parse(dateStr).toLocalDateTime();
            Integer matchday = (Integer) matchData.get("matchday");

            if (matchday == null) {
                System.out.println("⚠️ No matchday found for " + homeTeamName + " vs " + awayTeamName);
                return;
            }

            // Find or create GameWeek
            GameWeek gameWeek = gameweekRepository.findByWeekNumberAndCompetition(matchday, league)
                    .orElse(null);

            if (gameWeek == null) {
                System.out.println("🆕 Creating new GameWeek for " + league + " week " + matchday);
                gameWeek = new GameWeek();
                gameWeek.setWeekNumber(matchday);
                gameWeek.setCompetition(league);
                gameWeek.setStatus(GameweekStatus.UPCOMING);
                gameWeek.setStartDate(matchDateUtc.minusDays(3));
                gameWeek.setEndDate(matchDateUtc.plusDays(3));
                gameWeek.setJoinDeadline(matchDateUtc.minusHours(2));
                gameWeek = gameweekRepository.save(gameWeek);
            }

            // Parse score
            Integer homeGoals = null;
            Integer awayGoals = null;
            Map<String, Object> scoreMap = (Map<String, Object>) matchData.get("score");
            if (scoreMap != null) {
                Map<String, Object> fullTime = (Map<String, Object>) scoreMap.get("fullTime");
                if (fullTime != null) {
                    homeGoals = fullTime.get("home") != null ? (Integer) fullTime.get("home") : null;
                    awayGoals = fullTime.get("away") != null ? (Integer) fullTime.get("away") : null;
                }
            }

            // Find existing match or create new one
            Match dbMatch = matchRepository.findWithGameweeks(
                    homeTeamName, awayTeamName,
                    matchDateUtc.minusHours(2), matchDateUtc.plusHours(2)
            );

            if (dbMatch == null) {
                System.out.println("🆕 Creating new match: " + homeTeamName + " vs " + awayTeamName);
                dbMatch = new Match();
                dbMatch.setHomeTeam(homeTeamName);
                dbMatch.setAwayTeam(awayTeamName);
                dbMatch.setGameweeks(new ArrayList<>());
                dbMatch.setActive(true);
            } else {
                System.out.println("🔄 Updating existing match: " + homeTeamName + " vs " + awayTeamName);
                if (!dbMatch.isActive()) {
                    dbMatch.setActive(true);
                    System.out.println("✅ Match reactivated");
                }
            }

            // Update match data
            dbMatch.setMatchDate(matchDateUtc);
            dbMatch.setPredictionDeadline(matchDateUtc.minusMinutes(30));
            dbMatch.setHomeScore(homeGoals);
            dbMatch.setAwayScore(awayGoals);

            // Map API status to internal status
            switch (apiStatus) {
                case "FINISHED" -> {
                    dbMatch.setFinished(true);
                    dbMatch.setStatus(MatchStatus.COMPLETED);
                }
                case "IN_PLAY", "PAUSED" -> {
                    dbMatch.setFinished(false);
                    dbMatch.setStatus(MatchStatus.LIVE);
                }
                case "SCHEDULED", "TIMED", "POSTPONED" -> {
                    dbMatch.setFinished(false);
                    dbMatch.setStatus(MatchStatus.SCHEDULED);
                }
                default -> {
                    System.out.println("⚠️ Unknown match status: " + apiStatus);
                    dbMatch.setFinished(false);
                    dbMatch.setStatus(MatchStatus.SCHEDULED);
                }
            }

            // Link to GameWeek
            if (!dbMatch.getGameweeks().contains(gameWeek)) {
                dbMatch.getGameweeks().add(gameWeek);
            }

            // Save the match
            dbMatch = matchRepository.save(dbMatch);
            System.out.println("💾 Saved match: " + homeTeamName + " vs " + awayTeamName + " (Status: " + dbMatch.getStatus() + ")");

            affectedGameWeeks.add(gameWeek);

        } catch (Exception e) {
            System.out.println("❌ Error processing match data: " + e.getMessage());
            throw e;
        }
    }

    // ✅ Update GameWeek timing information based on its matches
    public void updateGameWeekTimings(GameWeek gameWeek) {
        try {
            List<Match> matches = matchRepository.findByGameweeksContaining(gameWeek);

            if (matches.isEmpty()) {
                System.out.println("⚠️ No matches found for GameWeek " + gameWeek.getWeekNumber());
                return;
            }

            // Find earliest and latest match dates
            LocalDateTime earliestMatch = matches.stream()
                    .map(Match::getMatchDate)
                    .filter(Objects::nonNull)
                    .min(LocalDateTime::compareTo)
                    .orElse(null);

            LocalDateTime latestMatch = matches.stream()
                    .map(Match::getMatchDate)
                    .filter(Objects::nonNull)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);

            if (earliestMatch != null && latestMatch != null) {
                gameWeek.setStartDate(earliestMatch.minusDays(1));
                gameWeek.setEndDate(latestMatch.plusDays(1));
                gameWeek.setJoinDeadline(earliestMatch.minusHours(2));

                System.out.println("📅 Updated GameWeek " + gameWeek.getWeekNumber() +
                        " timings: " + gameWeek.getStartDate().toLocalDate() +
                        " to " + gameWeek.getEndDate().toLocalDate());
            }

        } catch (Exception e) {
            System.out.println("⚠️ Error updating GameWeek timings: " + e.getMessage());
        }
    }

    // ✅ Update GameWeek statuses based on match completion
    private void updateGameweeks(Set<GameWeek> gameWeeks) {
        try {
            Set<GameWeek> newlyFinishedGameweeks = new HashSet<>();

            for (GameWeek gameWeek : gameWeeks) {
                List<Match> matches = matchRepository.findByGameweeksContaining(gameWeek);

                if (matches.isEmpty()) {
                    continue;
                }

                // Filter to only ACTIVE matches for status calculation
                List<Match> activeMatches = matches.stream()
                        .filter(Match::isActive)
                        .toList();

                if (activeMatches.isEmpty()) {
                    System.out.println("⚠️ No active matches for GameWeek " + gameWeek.getWeekNumber());
                    continue;
                }

                // Store previous status to detect changes
                GameweekStatus previousStatus = gameWeek.getStatus();

                // Calculate status based on ACTIVE match statuses only
                long finishedActiveMatches = activeMatches.stream()
                        .filter(Match::isFinished)
                        .count();

                long liveActiveMatches = activeMatches.stream()
                        .filter(m -> m.getStatus() == MatchStatus.LIVE)
                        .count();

                GameweekStatus newStatus;
                if (finishedActiveMatches == activeMatches.size()) {
                    // All active matches are finished
                    newStatus = GameweekStatus.FINISHED;
                } else if (liveActiveMatches > 0 || finishedActiveMatches > 0) {
                    // Some matches are live or finished but not all
                    newStatus = GameweekStatus.ONGOING;
                } else {
                    // No matches have started yet
                    newStatus = GameweekStatus.UPCOMING;
                }

                if (gameWeek.getStatus() != newStatus) {
                    gameWeek.setStatus(newStatus);
                    System.out.println("📊 GameWeek " + gameWeek.getWeekNumber() +
                            " status updated to " + newStatus +
                            " (Active: " + activeMatches.size() +
                            ", Finished: " + finishedActiveMatches +
                            ", Live: " + liveActiveMatches + ")");
                }

                // Track newly finished gameweeks
                if (previousStatus != GameweekStatus.FINISHED && newStatus == GameweekStatus.FINISHED) {
                    newlyFinishedGameweeks.add(gameWeek);
                    System.out.println("🏁 GameWeek " + gameWeek.getWeekNumber() + " just FINISHED!");
                }

                gameweekRepository.save(gameWeek);
            }

            // TRIGGER WINNER CALCULATION FOR NEWLY FINISHED GAMEWEEKS
            for (GameWeek finishedGameweek : newlyFinishedGameweeks) {
                triggerWinnerCalculation(finishedGameweek);
            }

        } catch (Exception e) {
            System.out.println("❌ Error updating gameweek statuses: " + e.getMessage());
        }
    }
    private void triggerWinnerCalculation(GameWeek gameWeek) {
        try {
            System.out.println("🏆 Starting winner calculation for finished GameWeek " + gameWeek.getWeekNumber());

            // Step 1: Score all predictions for matches in this gameweek
            List<Match> matches = matchRepository.findByGameweeksContaining(gameWeek);
            for (Match match : matches) {
                if (match.isActive() && match.isFinished()) {
                    predictionService.scorePredictionsForMatch(match.getId());
                    System.out.println("✅ Scored predictions for match: " +
                            match.getHomeTeam() + " vs " + match.getAwayTeam());
                }
            }

            // Step 2: Calculate accuracy for all participations in this gameweek
            List<SessionParticipation> participations =
                    predictionService.getSessionParticipationsByGameWeek(gameWeek.getId());

            for (SessionParticipation participation : participations) {
                predictionService.calculatePredictionAccuracy(participation.getId());
            }

            // Step 3: Determine winners for all sessions in this gameweek
            competitionSessionService.determineWinnersForCompletedGameWeek(gameWeek.getId());

            System.out.println("🎉 Winner calculation completed for GameWeek " + gameWeek.getWeekNumber());

        } catch (Exception e) {
            System.out.println("❌ Error calculating winners for GameWeek " + gameWeek.getWeekNumber() +
                    ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public LeagueTheme mapToLeagueTheme(String input) {
        if (input == null) return null;
        String normalized = input.toLowerCase().replace("_", " ");
        return switch (normalized) {
            case "premier league", "pl" -> LeagueTheme.PREMIER_LEAGUE;
            case "serie a", "sa" -> LeagueTheme.SERIE_A;
            case "champions league", "cl" -> LeagueTheme.CHAMPIONS_LEAGUE;
            case "europa league", "el" -> LeagueTheme.EUROPA_LEAGUE;
            case "bundesliga", "bl1" -> LeagueTheme.BUNDESLIGA;
            case "la liga", "laliga", "pd" -> LeagueTheme.LA_LIGA;
            case "ligue one", "fl1" -> LeagueTheme.LIGUE_ONE;
            case "bestoff", "bo" -> LeagueTheme.BESTOFF;
            case "conference league", "clg" -> LeagueTheme.CONFERENCE_LEAGUE;
            default -> null;
        };
    }

    String normalizeTeamName(String name) {
        if (name == null) return null;

        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase()
                .trim();

        normalized = normalized
                .replaceAll("\\b(fc|afc|rcd|cf)\\b", "")
                .replaceAll("\\bde futbol\\b", "")
                .replace("&", "and")
                .replaceAll("\\s+", " ")
                .trim();

        Map<String, String> teamMappings = Map.ofEntries(
                Map.entry("girona", "Girona"),
                Map.entry("rayo vallecano", "Rayo Vallecano"),
                Map.entry("villarreal", "Villarreal"),
                Map.entry("real oviedo", "Real Oviedo"),
                Map.entry("mallorca", "Mallorca"),
                Map.entry("barcelona", "Barcelona"),
                Map.entry("valencia", "Valencia"),
                Map.entry("real sociedad", "Real Sociedad"),
                Map.entry("celta vigo", "Celta Vigo"),
                Map.entry("getafe", "Getafe"),
                Map.entry("ath. bilbao", "Ath. Bilbao"),
                Map.entry("seville", "Seville"),
                Map.entry("espanyol", "Espanyol"),
                Map.entry("atletico madrid", "Atletico Madrid"),
                Map.entry("elche", "Elche"),
                Map.entry("real betis", "Real Betis"),
                Map.entry("real madrid", "Real Madrid"),
                Map.entry("osasuna", "Osasuna"),
                Map.entry("deportivo alaves", "Deportivo Alavés"),
                Map.entry("alaves", "Deportivo Alavés"),
                Map.entry("levante ud", "Levante"),
                Map.entry("espanyol de barcelona", "Espanyol"),
                Map.entry("club atletico de madrid", "Atletico Madrid"),
                Map.entry("rc celta de vigo", "Celta Vigo"),
                Map.entry("celta de vigo", "Celta Vigo"),
                Map.entry("athletic club", "Ath. Bilbao"),
                Map.entry("athletic bilbao", "Ath. Bilbao"),
                Map.entry("sevilla", "Seville"),
                Map.entry("real betis balompie", "Real Betis"),
                Map.entry("ca osasuna", "Osasuna"),
                //premier league
                Map.entry("arsenal", "Arsenal"),
                Map.entry("aston villa", "Aston Villa"),
                Map.entry("brentford", "Brentford"),
                Map.entry("brighton and hove albion", "Brighton and Hove Albion"),
                Map.entry("burnley", "Burnley"),
                Map.entry("chelsea", "Chelsea"),
                Map.entry("crystal palace", "Crystal Palace"),
                Map.entry("everton", "Everton"),
                Map.entry("fulham", "Fulham"),
                Map.entry("liverpool", "Liverpool"),
                Map.entry("leeds united", "Leeds United"),
                Map.entry("manchester city", "Manchester City"),
                Map.entry("manchester united", "Manchester United"),
                Map.entry("newcastle united", "Newcastle United"),
                Map.entry("nottingham forest", "Nottingham Forest"),
                Map.entry("sunderland", "Sunderland"),
                Map.entry("tottenham hotspur", "Tottenham Hotspur"),
                Map.entry("west ham united", "West Ham United"),
                Map.entry("wolverhampton wanderers", "Wolverhampton Wanderers"),
                Map.entry("bournemouth", "Bournemouth"),

                //ligue 1
                Map.entry("angers", "Angers"),
                Map.entry("auxerre", "Auxerre"),
                Map.entry("brest", "Brest"),
                Map.entry("le havre", "Le Havre"),
                Map.entry("lens", "Lens"),
                Map.entry("lille", "Lille"),
                Map.entry("lorient", "Lorient"),
                Map.entry("olympique lyonnais", "Olympique de Lyon"),
                Map.entry("olympique de marseille", "Olympique de Marseille"),
                Map.entry("metz", "Metz"),
                Map.entry("monaco", "Monaco"),
                Map.entry("nantes", "Nantes"),
                Map.entry("nice", "Nice"),
                Map.entry("paris fc", "Paris FC"),
                Map.entry("paris saint-germain", "Paris Saint-Germain"),
                Map.entry("stade rennais 1901", "Rennes"),
                Map.entry("strasbourg", "Strasbourg"),
                Map.entry("toulouse", "Toulouse"),


                //serie A
                Map.entry("ac milan", "AC Milan"),
                Map.entry("atalanta", "Atalanta"),
                Map.entry("bologna", "Bologna"),
                Map.entry("cagliari", "Cagliari"),
                Map.entry("como", "Como"),
                Map.entry("cremonese", "Cremonese"),
                Map.entry("fiorentina", "Fiorentina"),
                Map.entry("genoa", "Genoa"),
                Map.entry("inter", "Inter"),
                Map.entry("juventus", "Juventus"),
                Map.entry("lazio", "Lazio"),
                Map.entry("lecce", "Lecce"),
                Map.entry("napoli", "Napoli"),
                Map.entry("parma", "Parma"),
                Map.entry("pisa", "Pisa"),
                Map.entry("roma", "Roma"),
                Map.entry("sassuolo", "Sassuolo"),
                Map.entry("torino", "Torino"),
                Map.entry("udinese", "Udinese"),
                Map.entry("hellas verona", "Hellas Verona"),

                Map.entry("augsburg", "Augsburg"),
                Map.entry("bayer 04 leverkusen", "Bayer Leverkusen"),
                Map.entry("bayern munchen", "Bayern Munich"),
                Map.entry("borussia dortmund", "Borussia Dortmund"),
                Map.entry("borussia borussia monchengladbach", "Borussia Mönchengladbach"),
                Map.entry("eintracht frankfurt", "Eintracht Frankfurt"),
                Map.entry("freiburg", "Freiburg"),
                Map.entry("heidenheim", "Heidenheim"),
                Map.entry("hoffenheim", "Hoffenheim"),
                Map.entry("koln", "Koln"),
                Map.entry("mainz", "Mainz"),
                Map.entry("rb leipzig", "RB Leipzig"),
                Map.entry("stuttgart", "Stuttgart"),
                Map.entry("union berlin", "Union Berlin"),
                Map.entry("werder bremen", "Werder Bremen"),
                Map.entry("wolfsburg", "Wolfsburg"),
                Map.entry("hamburg", "Hamburg"),
                Map.entry("st. pauli 1910", "St Pauli"),

                Map.entry("psv", "PSV Eindhoven")
        );

        if (teamMappings.containsKey(normalized)) {
            return teamMappings.get(normalized);
        }

        for (Map.Entry<String, String> entry : teamMappings.entrySet()) {
            if (normalized.contains(entry.getKey()) || entry.getKey().contains(normalized)) {
                return entry.getValue();
            }
        }

        return normalized;
    }
}