package FootballFantasy.fantasy.Services.DataService;

import FootballFantasy.fantasy.Entities.GameweekEntity.*;
import FootballFantasy.fantasy.Repositories.GameweekRepository.GameWeekRepository;
import FootballFantasy.fantasy.Repositories.GameweekRepository.MatchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
@Service
public class MatchUpdateService {

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private GameWeekRepository gameweekRepository;

    @Value("${football.api.key}")
    private String apiKey;

    @Value("${football.api.url-template}")
    private String apiUrlTemplate;

    // ✅ Automatic update every 2 hours
    @Scheduled(cron = "0 0 */2 * * *")
    public void updateMatchesAutomatically() {
        updateMatches(); // reuse the same method for all leagues
    }

    // ✅ Public method to update all leagues
    public void updateMatches() {
        for (LeagueTheme league : LeagueTheme.values()) {
            try {
                System.out.println("➡️ Updating matches for competition: " + league.name());
                fetchAndUpdateMatches(league.getApiCode(), 2025);
            } catch (Exception e) {
                System.out.println("❌ Exception while updating " + league.name() + ": " + e.getMessage());
            }
        }
    }

    // ✅ Public method to update a specific league manually
    public void updateMatchesManually(String competition) {
        try {
            LeagueTheme league = LeagueTheme.valueOf(competition);
            fetchAndUpdateMatches(league.getApiCode(), 2025);
        } catch (IllegalArgumentException e) {
            System.out.println("❌ Invalid competition: " + competition);
        }
    }

    public void fetchAndUpdateMatches(String competitionCode, int season) {
        try {
            String apiUrl = apiUrlTemplate
                    .replace("{competition}", competitionCode)
                    .replace("{season}", String.valueOf(season));

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Auth-Token", apiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(apiUrl, HttpMethod.GET, entity, Map.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                System.out.println("⚠️ API call failed for " + competitionCode + " with status: " + response.getStatusCode());
                return;
            }

            List<Map<String, Object>> matches = (List<Map<String, Object>>) response.getBody().get("matches");
            System.out.println("➡️ " + matches.size() + " matches received from " + competitionCode);

            Set<GameWeek> affectedGameWeeks = new HashSet<>();

            for (Map<String, Object> matchData : matches) {
                processMatchData(matchData, affectedGameWeeks);
            }

            updateGameweeks(affectedGameWeeks);

        } catch (Exception e) {
            System.out.println("❌ Error fetching " + competitionCode + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void processMatchData(Map<String, Object> matchData, Set<GameWeek> affectedGameWeeks) {
        Map<String, Object> homeTeamMap = (Map<String, Object>) matchData.get("homeTeam");
        Map<String, Object> awayTeamMap = (Map<String, Object>) matchData.get("awayTeam");

        String homeTeamName = normalizeTeamName((String) homeTeamMap.get("name"));
        String awayTeamName = normalizeTeamName((String) awayTeamMap.get("name"));
        String apiStatus = (String) matchData.get("status");

        if (!"FINISHED".equals(apiStatus)) {
            System.out.println("⏭ Skipping match (not finished): " + homeTeamName + " vs " + awayTeamName);
            return;
        }

        String dateStr = (String) matchData.get("utcDate");
        LocalDateTime matchDateUtc = OffsetDateTime.parse(dateStr).toLocalDateTime();

        Map<String, Object> scoreMap = (Map<String, Object>) matchData.get("score");
        Map<String, Object> fullTime = (Map<String, Object>) scoreMap.get("fullTime");
        Integer homeGoals = fullTime.get("home") != null ? (Integer) fullTime.get("home") : 0;
        Integer awayGoals = fullTime.get("away") != null ? (Integer) fullTime.get("away") : 0;

        LocalDateTime from = matchDateUtc.minusHours(1);
        LocalDateTime to = matchDateUtc.plusHours(1);

        Match dbMatch = matchRepository.findWithGameweeks(homeTeamName, awayTeamName, from, to);

        if (dbMatch != null) {
            dbMatch.setMatchDate(matchDateUtc);
            dbMatch.setHomeScore(homeGoals);
            dbMatch.setAwayScore(awayGoals);
            dbMatch.setPredictionDeadline(matchDateUtc.minusMinutes(30));
            dbMatch.setFinished(true);
            dbMatch.setStatus(MatchStatus.COMPLETED);

            matchRepository.save(dbMatch);
            affectedGameWeeks.addAll(dbMatch.getGameweeks());
            System.out.println("✅ Updated match: " + homeTeamName + " " + homeGoals + "-" + awayGoals + " " + awayTeamName);
        } else {
            System.out.println("❌ No matching DB match found for: " + homeTeamName + " vs " + awayTeamName);
        }
    }

    private void updateGameweeks(Set<GameWeek> affectedGameWeeks) {
        for (GameWeek gw : affectedGameWeeks) {
            List<Match> gwMatches = matchRepository.findByGameweeksId(gw.getId());
            long total = gwMatches.stream().filter(Match::isActive).count();
            long completed = gwMatches.stream().filter(m -> m.getStatus() == MatchStatus.COMPLETED).count();

            if (completed == 0) gw.setStatus(GameweekStatus.UPCOMING);
            else if (completed < total) gw.setStatus(GameweekStatus.ONGOING);
            else gw.setStatus(GameweekStatus.FINISHED);

            gameweekRepository.save(gw);
            System.out.println("🟢 Updated GameWeek " + gw.getId() + " status: " + gw.getStatus());
        }
    }

    private String normalizeTeamName(String name) {
        if (name == null) return null;

        // Step 1: Normalize string - lowercase, remove accents, trim spaces
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase()
                .trim();

        // Step 2: Remove common football prefixes/suffixes safely
        normalized = normalized
                .replaceAll("\\b(fc|afc|rcd|cf)\\b", "") // leave 'ud' for now, handle in mapping
                .replaceAll("\\bde futbol\\b", "")
                .replace("&", "and")
                .replaceAll("\\s+", " ")
                .trim();

        // Step 3: Map normalized API names to exact DB names
        Map<String, String> teamMappings = Map.ofEntries(
                // Spanish league DB exact names
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
                Map.entry("levante ud", "Levante UD"), // DB exact
                Map.entry("levante", "Levante UD"),    // alternative

                // Other API variations
                Map.entry("espanyol de barcelona", "Espanyol"),
                Map.entry("club atletico de madrid", "Atletico Madrid"),
                Map.entry("rc celta de vigo", "Celta Vigo"),
                Map.entry("celta de vigo", "Celta Vigo"),
                Map.entry("athletic club", "Ath. Bilbao"),
                Map.entry("athletic bilbao", "Ath. Bilbao"),
                Map.entry("sevilla", "Seville"),
                Map.entry("real betis balompie", "Real Betis"),
                Map.entry("ca osasuna", "Osasuna")
        );

        // Debug output
        System.out.println("Original: '" + name + "'");
        System.out.println("Normalized: '" + normalized + "'");

        // Step 4: Exact match first
        if (teamMappings.containsKey(normalized)) {
            return teamMappings.get(normalized);
        }

        // Step 5: Partial match for complex names
        for (Map.Entry<String, String> entry : teamMappings.entrySet()) {
            if (normalized.contains(entry.getKey()) || entry.getKey().contains(normalized)) {
                return entry.getValue();
            }
        }

        // Step 6: Return normalized if no match found
        return normalized;
    }


    }
