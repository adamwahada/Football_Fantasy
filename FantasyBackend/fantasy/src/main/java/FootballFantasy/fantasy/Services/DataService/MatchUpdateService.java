package FootballFantasy.fantasy.Services.DataService;

import FootballFantasy.fantasy.Entities.GameweekEntity.GameWeek;
import FootballFantasy.fantasy.Entities.GameweekEntity.GameweekStatus;
import FootballFantasy.fantasy.Entities.GameweekEntity.Match;
import FootballFantasy.fantasy.Entities.GameweekEntity.MatchStatus;
import FootballFantasy.fantasy.Repositories.GameweekRepository.GameWeekRepository;
import FootballFantasy.fantasy.Repositories.GameweekRepository.MatchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class MatchUpdateService {

    @Autowired
    private MatchRepository matchRepository;
    @Autowired
    private GameWeekRepository gameweekRepository;

    private final String API_URL = "https://api.football-data.org/v4/competitions/PL/matches?season=2025";
    private final String API_KEY = "3aad9be232cd4b17800a5ec5a9b9986d";

    // Runs every 2 hours
    @Scheduled(cron = "0 0 */2 * * *")
    public void updateMatches() {
        try {
            System.out.println("➡️ Triggering MatchUpdateService...");

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Auth-Token", API_KEY);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(API_URL, HttpMethod.GET, entity, Map.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                System.out.println("⚠️ API call failed with status: " + response.getStatusCode());
                return;
            }

            List<Map<String, Object>> matches = (List<Map<String, Object>>) response.getBody().get("matches");
            System.out.println("➡️ Number of matches received from API: " + matches.size());

            Set<GameWeek> affectedGameWeeks = new HashSet<>();

            for (Map<String, Object> matchData : matches) {
                Map<String, Object> homeTeamMap = (Map<String, Object>) matchData.get("homeTeam");
                Map<String, Object> awayTeamMap = (Map<String, Object>) matchData.get("awayTeam");

                // Normalize team names before DB search
                String homeTeamName = normalizeTeamName((String) homeTeamMap.get("name"));
                String awayTeamName = normalizeTeamName((String) awayTeamMap.get("name"));

                String apiStatus = (String) matchData.get("status");

                // Skip matches that are not finished
                if (!"FINISHED".equals(apiStatus)) {
                    System.out.println("⏭ Skipping match (not finished): " + homeTeamName + " vs " + awayTeamName + " | Status: " + apiStatus);
                    continue;
                }

                // Parse match date
                String dateStr = (String) matchData.get("utcDate");
                LocalDateTime matchDateUtc = OffsetDateTime.parse(dateStr).toLocalDateTime();

                // Parse full-time goals safely
                Map<String, Object> scoreMap = (Map<String, Object>) matchData.get("score");
                Map<String, Object> fullTime = (Map<String, Object>) scoreMap.get("fullTime");
                Integer homeGoals = fullTime.get("home") != null ? (Integer) fullTime.get("home") : 0;
                Integer awayGoals = fullTime.get("away") != null ? (Integer) fullTime.get("away") : 0;

                MatchStatus dbStatus = MatchStatus.COMPLETED;

                // ±1 hour window for DB match lookup
                LocalDateTime from = matchDateUtc.minusHours(1);
                LocalDateTime to = matchDateUtc.plusHours(1);

                System.out.println("Looking for DB match: " + homeTeamName + " vs " + awayTeamName +
                        " between " + from + " and " + to + " | Status: " + apiStatus +
                        " | Goals: " + homeGoals + "-" + awayGoals);

                Match dbMatch = matchRepository.findWithGameweeks(
                        homeTeamName, awayTeamName, from, to
                );

                if (dbMatch != null) {
                    System.out.println("✅ Match found in DB: " + dbMatch.getHomeTeam() + " vs " + dbMatch.getAwayTeam() +
                            " | Previous score: " + dbMatch.getHomeScore() + "-" + dbMatch.getAwayScore());

                    dbMatch.setMatchDate(matchDateUtc);
                    dbMatch.setHomeScore(homeGoals);
                    dbMatch.setAwayScore(awayGoals);
                    dbMatch.setPredictionDeadline(matchDateUtc.minusMinutes(30));
                    dbMatch.setFinished(true);
                    dbMatch.setStatus(dbStatus);

                    matchRepository.save(dbMatch);
                    affectedGameWeeks.addAll(dbMatch.getGameweeks());

                    System.out.println("✅ Updated match: " + homeTeamName + " " + homeGoals + "-" + awayGoals + " " + awayTeamName);
                } else {
                    System.out.println("❌ No matching DB match found for: " + homeTeamName + " vs " + awayTeamName +
                            " around " + matchDateUtc);
                }
            }

            // Update affected gameweeks
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

            System.out.println("➡️ MatchUpdateService finished successfully.");

        } catch (Exception e) {
            System.out.println("❌ Exception while updating matches: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Normalize team names for DB lookup
    private String normalizeTeamName(String name) {
        if (name == null) return null;
        return name.replaceAll("(?i)\\bFC\\b", "")
                .replaceAll("(?i)\\bAFC\\b", "")
                .trim();
    }
}
