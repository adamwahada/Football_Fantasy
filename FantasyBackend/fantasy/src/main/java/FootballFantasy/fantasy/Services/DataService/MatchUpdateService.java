package FootballFantasy.fantasy.Services.DataService;

import FootballFantasy.fantasy.Entities.GameweekEntity.*;
import FootballFantasy.fantasy.Repositories.GameweekRepository.GameWeekRepository;
import FootballFantasy.fantasy.Repositories.GameweekRepository.MatchRepository;
import jakarta.transaction.Transactional;
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
        updateMatches();
    }

    // ✅ Update all leagues
    public void updateMatches() {
        for (LeagueTheme league : LeagueTheme.values()) {
            try {
                if (!league.isApiAvailable()) {
                    System.out.println("⏭️ Skipping league not available in API: " + league.name());
                    continue;
                }

                System.out.println("➡️ Updating matches for competition: " + league.name());
                fetchAndUpdateMatches(league, 2025);

            } catch (Exception e) {
                System.out.println("❌ Exception while updating " + league.name() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }


    // ✅ Update specific league manually
    public void updateMatchesManually(String competition) {
        try {
            LeagueTheme league = LeagueTheme.valueOf(competition);
            fetchAndUpdateMatches(league, 2025);
        } catch (IllegalArgumentException e) {
            System.out.println("❌ Invalid competition: " + competition);
        }
    }

    private void fetchAndUpdateMatches(LeagueTheme league, int season) {
        try {
            String apiUrl = apiUrlTemplate
                    .replace("{competition}", league.getApiCode())
                    .replace("{season}", String.valueOf(season));

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Auth-Token", apiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(apiUrl, HttpMethod.GET, entity, Map.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                System.out.println("⚠️ API call failed for " + league + " with status: " + response.getStatusCode());
                return;
            }

            List<Map<String, Object>> matches = (List<Map<String, Object>>) response.getBody().get("matches");
            System.out.println("➡️ " + matches.size() + " matches received from " + league);

            // Keep track of affected GameWeeks
            Set<GameWeek> affectedGameWeeks = new HashSet<>();

            for (Map<String, Object> matchData : matches) {
                processMatchData(matchData, league, affectedGameWeeks);
            }

            // 🔹 Step 3: Update GameWeek timings only once per GameWeek
            for (GameWeek gw : affectedGameWeeks) {
                updateGameWeekTimings(gw); // sets startDate, endDate, joinDeadline
            }

            // Update GameWeek statuses (UPCOMING, ONGOING, FINISHED)
            updateGameweeks(affectedGameWeeks);

        } catch (Exception e) {
            System.out.println("❌ Error fetching " + league + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void processMatchData(Map<String, Object> matchData, LeagueTheme league, Set<GameWeek> affectedGameWeeks) {
        Map<String, Object> homeTeamMap = (Map<String, Object>) matchData.get("homeTeam");
        Map<String, Object> awayTeamMap = (Map<String, Object>) matchData.get("awayTeam");

        String homeTeamName = normalizeTeamName((String) homeTeamMap.get("name"));
        String awayTeamName = normalizeTeamName((String) awayTeamMap.get("name"));
        String apiStatus = (String) matchData.get("status");

        String dateStr = (String) matchData.get("utcDate");
        LocalDateTime matchDateUtc = OffsetDateTime.parse(dateStr).toLocalDateTime();

        Integer matchday = (Integer) matchData.get("matchday");
        if (matchday == null) {
            System.out.println("⚠️ No matchday found for " + homeTeamName + " vs " + awayTeamName);
            return;
        }

        // ✅ Find or create GameWeek
        GameWeek gameWeek = gameweekRepository
                .findByWeekNumberAndCompetition(matchday, league)
                .orElseGet(() -> {
                    GameWeek gw = new GameWeek();
                    gw.setWeekNumber(matchday);
                    gw.setCompetition(league);
                    gw.setStatus(GameweekStatus.UPCOMING);
                    // Initial timing setup (will be updated later in batch)
                    gw.setStartDate(matchDateUtc.minusDays(3));
                    gw.setEndDate(matchDateUtc.plusDays(3));
                    gw.setJoinDeadline(matchDateUtc.minusHours(2));
                    return gameweekRepository.save(gw);
                });

        // Parse score
        Map<String, Object> scoreMap = (Map<String, Object>) matchData.get("score");
        Map<String, Object> fullTime = (Map<String, Object>) scoreMap.get("fullTime");
        Integer homeGoals = fullTime.get("home") != null ? (Integer) fullTime.get("home") : null;
        Integer awayGoals = fullTime.get("away") != null ? (Integer) fullTime.get("away") : null;

        // Create or update match
        Match dbMatch = matchRepository.findWithGameweeks(
                homeTeamName, awayTeamName,
                matchDateUtc.minusHours(2), matchDateUtc.plusHours(2)
        );

        if (dbMatch == null) {
            dbMatch = new Match();
            dbMatch.setHomeTeam(homeTeamName);
            dbMatch.setAwayTeam(awayTeamName);
            dbMatch.setGameweeks(new ArrayList<>());
        }

        dbMatch.setMatchDate(matchDateUtc);
        dbMatch.setPredictionDeadline(matchDateUtc.minusMinutes(30));
        dbMatch.setHomeScore(homeGoals);
        dbMatch.setAwayScore(awayGoals);

        // Status mapping
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
        }

        // 🔹 FIX: Ensure we're working with a managed GameWeek entity
        // Re-fetch the gameweek to ensure it's managed by the current session
        GameWeek managedGameWeek = gameweekRepository.findById(gameWeek.getId()).orElse(gameWeek);

        if (!dbMatch.getGameweeks().contains(managedGameWeek)) {
            dbMatch.getGameweeks().add(managedGameWeek);
        }

        matchRepository.save(dbMatch);
        affectedGameWeeks.add(managedGameWeek);

        System.out.println("✅ Synced match: " + homeTeamName + " vs " + awayTeamName + " (" + apiStatus + ")");
    }

    private void updateGameweeks(Set<GameWeek> affectedGameWeeks) {
        for (GameWeek gw : affectedGameWeeks) {
            List<Match> gwMatches = matchRepository.findByGameweeksId(gw.getId());
            long total = gwMatches.size();
            long completed = gwMatches.stream().filter(m -> m.getStatus() == MatchStatus.COMPLETED).count();

            if (completed == 0) gw.setStatus(GameweekStatus.UPCOMING);
            else if (completed < total) gw.setStatus(GameweekStatus.ONGOING);
            else gw.setStatus(GameweekStatus.FINISHED);

            gameweekRepository.save(gw);
            System.out.println("🟢 Updated GameWeek " + gw.getWeekNumber() + " (" + gw.getCompetition() + ") → " + gw.getStatus());
        }
    }
    private void updateGameWeekTimings(GameWeek gameWeek) {
        List<Match> matches = matchRepository.findByGameweeksId(gameWeek.getId());
        if (matches.isEmpty()) return;

        // Start date = earliest match
        LocalDateTime start = matches.stream()
                .map(Match::getMatchDate)
                .min(LocalDateTime::compareTo)
                .orElseThrow();

        // End date = latest match + 2h30
        LocalDateTime end = matches.stream()
                .map(Match::getMatchDate)
                .max(LocalDateTime::compareTo)
                .orElseThrow()
                .plusHours(2).plusMinutes(30);

        // Join deadline = 30 minutes before first match
        LocalDateTime joinDeadline = start.minusMinutes(30);

        gameWeek.setStartDate(start);
        gameWeek.setEndDate(end);
        gameWeek.setJoinDeadline(joinDeadline);

        gameweekRepository.save(gameWeek);
    }

    @Transactional

    public void updateMatchesForGameweek(String competition, int weekNumber) {
        try {
            System.out.println("🔍 DEBUG: Received competition: '" + competition + "', weekNumber: " + weekNumber);

            // Map user input to LeagueTheme enum
            LeagueTheme league = mapToLeagueTheme(competition);

            if (league == null) {
                System.out.println("❌ Invalid competition: " + competition);
                System.out.println("🔍 Available competitions: " + Arrays.toString(LeagueTheme.values()));
                return;
            }

            System.out.println("✅ Mapped to league: " + league + " (API code: " + league.getApiCode() + ")");

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
                System.out.println("⚠️ API call failed for " + league + " with status: " + response.getStatusCode());
                if (response.getBody() != null) {
                    System.out.println("Response body: " + response.getBody());
                }
                return;
            }

            List<Map<String, Object>> matches = (List<Map<String, Object>>) response.getBody().get("matches");
            System.out.println("🔍 Total matches received: " + (matches != null ? matches.size() : 0));

            if (matches == null) {
                System.out.println("⚠️ No matches found in API response");
                return;
            }

            // Filter only matches from the requested week
            List<Map<String, Object>> filteredMatches = matches.stream()
                    .filter(m -> {
                        Integer matchday = (Integer) m.get("matchday");
                        boolean isMatch = Objects.equals(matchday, weekNumber);
                        if (isMatch) {
                            System.out.println("🔍 Found match for week " + weekNumber + ": " +
                                    m.get("homeTeam") + " vs " + m.get("awayTeam"));
                        }
                        return isMatch;
                    })
                    .toList();

            System.out.println("➡️ " + filteredMatches.size() + " matches found for "
                    + league + " week " + weekNumber);

            if (filteredMatches.isEmpty()) {
                System.out.println("⚠️ No matches found for week " + weekNumber + " in " + league);
                return;
            }

            Set<GameWeek> affectedGameWeeks = new HashSet<>();

            for (Map<String, Object> matchData : filteredMatches) {
                processMatchData(matchData, league, affectedGameWeeks);
            }

            // Update GameWeek timings & statuses
            for (GameWeek gw : affectedGameWeeks) {
                updateGameWeekTimings(gw);
            }
            updateGameweeks(affectedGameWeeks);

            System.out.println("✅ Successfully updated " + affectedGameWeeks.size() + " gameweek(s)");

        } catch (Exception e) {
            System.out.println("❌ Error fetching matches for " + competition + " week " + weekNumber + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    // Helper method to map user-friendly strings to enum
    public LeagueTheme mapToLeagueTheme(String input) {
        if (input == null) return null;
        String normalized = input.toLowerCase().replace("_", " "); // <-- fix
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
                Map.entry("st. pauli 1910", "St Pauli")

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
