package FootballFantasy.fantasy.Services.GameweekService;

import FootballFantasy.fantasy.Entities.GameweekEntity.GameweekStatus;
import FootballFantasy.fantasy.Entities.GameweekEntity.*;
import FootballFantasy.fantasy.Repositories.GameweekRepository.GameWeekRepository;
import FootballFantasy.fantasy.Repositories.GameweekRepository.MatchRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class GameWeekService {

    @Autowired
    private GameWeekRepository gameWeekRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private PredictionService predictionService;

    /**
     * Helper method to find or create a match to prevent duplicates
     */
    private Match findOrCreateMatch(Match matchData) {
        // Try to find existing match by home team, away team, and match date
        Match existingMatch = matchRepository.findByHomeTeamAndAwayTeamAndMatchDate(
                matchData.getHomeTeam(),
                matchData.getAwayTeam(),
                matchData.getMatchDate()
        );

        if (existingMatch != null) {
            // Update existing match with new data
            existingMatch.setMatchDate(matchData.getMatchDate());

            if (matchData.getHomeScore() != null) {
                existingMatch.setHomeScore(matchData.getHomeScore());
            }
            if (matchData.getAwayScore() != null) {
                existingMatch.setAwayScore(matchData.getAwayScore());
            }

            // Respect input 'finished' and 'status' if provided; otherwise fallback
            if (matchData.getStatus() != null) {
                existingMatch.setStatus(matchData.getStatus());
            } else {
                boolean isCompleted = existingMatch.getHomeScore() != null && existingMatch.getAwayScore() != null;
                existingMatch.setStatus(isCompleted ? MatchStatus.COMPLETED : MatchStatus.SCHEDULED);
            }

            existingMatch.setFinished(matchData.isFinished());

            // Update prediction deadline based on match date
            existingMatch.setPredictionDeadline(existingMatch.getMatchDate().minusMinutes(30));

            return existingMatch;
        } else {
            // Create new match

            // Use input values for finished and status, or fallback if null
            if (matchData.getStatus() == null) {
                boolean isCompleted = matchData.getHomeScore() != null && matchData.getAwayScore() != null;
                matchData.setStatus(isCompleted ? MatchStatus.COMPLETED : MatchStatus.SCHEDULED);
            }

            // If finished was not set in input, set it based on status
            if (!matchData.isFinished()) {
                matchData.setFinished(matchData.getStatus() == MatchStatus.COMPLETED);
            }

            matchData.setPredictionDeadline(matchData.getMatchDate().minusMinutes(30));
            return matchData;
        }
    }

    /**
     * Helper method to safely link a match to a gameweek
     * A match can belong to multiple gameweeks (e.g., Premier League + Best Of)
     */
    private void linkMatchToGameWeek(Match match, GameWeek gameWeek) {
        // Add gameweek to match's gameweeks collection (if not already present)
        if (!match.getGameweeks().contains(gameWeek)) {
            match.getGameweeks().add(gameWeek);
        }

        // Add match to gameweek's matches collection (if not already present)
        if (!gameWeek.getMatches().contains(match)) {
            gameWeek.getMatches().add(match);
        }
    }

    public GameWeek createGameWeek(GameWeek gameWeek) {
        return gameWeekRepository.save(gameWeek);
    }

    public GameWeek updateGameWeek(Long gameWeekId, GameWeek updatedGameWeek) {
        GameWeek existingGameWeek = gameWeekRepository.findById(gameWeekId)
                .orElseThrow(() -> new IllegalArgumentException("GameWeek non trouvé"));

        existingGameWeek.setDescription(updatedGameWeek.getDescription());
        existingGameWeek.setStatus(updatedGameWeek.getStatus());
        existingGameWeek.setWeekNumber(updatedGameWeek.getWeekNumber());
        existingGameWeek.setCompetition(updatedGameWeek.getCompetition());

        return gameWeekRepository.save(existingGameWeek);
    }

    public void deleteGameWeek(Long gameWeekId) {
        GameWeek gameWeek = gameWeekRepository.findById(gameWeekId)
                .orElseThrow(() -> new RuntimeException("GameWeek not found"));

        for (Match match : gameWeek.getMatches()) {
            match.getGameweeks().remove(gameWeek);
        }
        gameWeek.getMatches().clear();
        gameWeekRepository.delete(gameWeek);
    }

    @Transactional
    public Match addMatchToGameWeek(Long gameWeekId, Match matchData) {
        GameWeek gameWeek = gameWeekRepository.findById(gameWeekId)
                .orElseThrow(() -> new IllegalArgumentException("GameWeek non trouvé"));

        if (matchData.getMatchDate().isBefore(gameWeek.getStartDate()) ||
                matchData.getMatchDate().isAfter(gameWeek.getEndDate())) {
            throw new IllegalArgumentException("La date du match doit être comprise entre la date de début et de fin de la GameWeek.");
        }

        // Use helper method to find or create match (prevents duplicates)
        Match match = findOrCreateMatch(matchData);

        // Link match to gameweek
        linkMatchToGameWeek(match, gameWeek);

        // Save the match
        match = matchRepository.save(match);

        recalculateGameWeekDates(gameWeek);

        // Update status of all affected gameweeks
        updateAllAffectedGameWeekStatuses(List.of(match));
        return match;
    }

    public List<Match> getMatchesByGameWeek(Long gameWeekId) {
        return matchRepository.findByGameweeksId(gameWeekId);
    }

    @Transactional
    public void deleteMatchesByGameWeek(Long gameWeekId) {
        GameWeek gameWeek = gameWeekRepository.findById(gameWeekId)
                .orElseThrow(() -> new IllegalArgumentException("GameWeek not found"));

        for (Match match : new ArrayList<>(gameWeek.getMatches())) {
            match.getGameweeks().remove(gameWeek);
            gameWeek.getMatches().remove(match);
            matchRepository.save(match);
        }

        gameWeekRepository.save(gameWeek);
        recalculateGameWeekDates(gameWeek);
    }

    @Transactional
    public void deleteSpecificMatchesFromGameWeek(Long gameWeekId, List<Long> matchIdsToRemove) {
        GameWeek gameWeek = gameWeekRepository.findById(gameWeekId)
                .orElseThrow(() -> new IllegalArgumentException("GameWeek not found"));

        for (Match match : new ArrayList<>(gameWeek.getMatches())) {
            if (matchIdsToRemove.contains(match.getId())) {
                match.getGameweeks().remove(gameWeek);
                gameWeek.getMatches().remove(match);
                matchRepository.save(match);
            }
        }
        gameWeekRepository.save(gameWeek);
        recalculateGameWeekDates(gameWeek);
    }

    @Transactional
    public Match linkExistingMatchToGameWeek(Long gameWeekId, Long matchId) {
        GameWeek gameWeek = gameWeekRepository.findById(gameWeekId)
                .orElseThrow(() -> new IllegalArgumentException("GameWeek not found"));

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found"));

        // Use helper method to link (prevents duplicate linking)
        linkMatchToGameWeek(match, gameWeek);

        matchRepository.save(match);
        gameWeekRepository.save(gameWeek);

        recalculateGameWeekDates(gameWeek);
        return match;
    }

    @Transactional
    public List<Match> linkMultipleMatchesToGameWeek(Long gameWeekId, List<Long> matchIds) {
        GameWeek gameWeek = gameWeekRepository.findById(gameWeekId)
                .orElseThrow(() -> new IllegalArgumentException("GameWeek not found"));

        List<Match> updatedMatches = new ArrayList<>();

        for (Long matchId : matchIds) {
            Match match = matchRepository.findById(matchId)
                    .orElseThrow(() -> new IllegalArgumentException("Match not found with ID: " + matchId));

            // Use helper method to link (prevents duplicate linking)
            linkMatchToGameWeek(match, gameWeek);
            updatedMatches.add(matchRepository.save(match));
        }

        gameWeekRepository.save(gameWeek);
        recalculateGameWeekDates(gameWeek);
        return updatedMatches;
    }

    public GameWeek getByWeekNumber(int weekNumber) {
        return gameWeekRepository.findByWeekNumber(weekNumber)
                .orElseThrow(() -> new IllegalArgumentException("GameWeek not found for week number " + weekNumber));
    }

    public boolean isGameWeekComplete(Long gameWeekId) {
        List<Match> matches = matchRepository.findByGameweeksId(gameWeekId);

        return matches.stream()
                .filter(Match::isActive)
                .allMatch(m -> m.getStatus() == MatchStatus.COMPLETED);
    }

    public void recalculateGameWeekDates(GameWeek gameWeek) {
        List<Match> matches = gameWeek.getMatches();
        if (matches.isEmpty()) {
            gameWeek.setStartDate(null);
            gameWeek.setEndDate(null);
            gameWeek.setJoinDeadline(null);
        } else {
            LocalDateTime earliest = matches.stream()
                    .map(Match::getMatchDate)
                    .min(LocalDateTime::compareTo)
                    .orElseThrow();

            LocalDateTime latest = matches.stream()
                    .map(Match::getMatchDate)
                    .max(LocalDateTime::compareTo)
                    .orElseThrow();

            gameWeek.setStartDate(earliest);
            gameWeek.setEndDate(latest.plusHours(2).plusMinutes(30));
            gameWeek.setJoinDeadline(earliest.minusMinutes(30));
        }

        gameWeekRepository.save(gameWeek);
    }

    /**
     * Helper method to update status of all gameweeks that contain the given matches
     */
    private void updateAllAffectedGameWeekStatuses(List<Match> updatedMatches) {
        Set<GameWeek> affectedGameWeeks = new HashSet<>();
        for (Match match : updatedMatches) {
            affectedGameWeeks.addAll(match.getGameweeks());
        }

        for (GameWeek gameWeek : affectedGameWeeks) {
            System.out.println("🔄 Checking status for affected gameweek ID: " + gameWeek.getId());

            List<Match> gameweekMatches = matchRepository.findByGameweeksId(gameWeek.getId());
            boolean allCompleted = gameweekMatches.stream()
                    .filter(Match::isActive)
                    .allMatch(m -> m.getStatus() == MatchStatus.COMPLETED);

            GameweekStatus newStatus = allCompleted ? GameweekStatus.FINISHED : GameweekStatus.ONGOING;

            if (gameWeek.getStatus() != newStatus) {
                System.out.println("📊 Updating gameweek " + gameWeek.getId() + " status from " +
                        gameWeek.getStatus() + " to " + newStatus);
                gameWeek.setStatus(newStatus);
                gameWeekRepository.save(gameWeek);

                // ✅ USE YOUR EXISTING METHOD
                if (newStatus == GameweekStatus.FINISHED) {
                    triggerPostGameWeekFinishedActions(gameWeek);
                }
            } else {
                System.out.println("✓ Gameweek " + gameWeek.getId() + " status unchanged (" + newStatus + ")");
            }
        }
    }
    private void triggerPostGameWeekFinishedActions(GameWeek gameWeek) {
        System.out.println("🏆 GameWeek " + gameWeek.getId() + " finished. Triggering post-processing...");

        // 1. Find all session participations for this gameweek
        List<SessionParticipation> participations = predictionService
                .getSessionParticipationsByGameWeek(gameWeek.getId());

        // 2. For each participation, calculate accuracy
        for (SessionParticipation participation : participations) {
            predictionService.calculatePredictionAccuracy(participation.getId());
        }

        // 3. Determine winners per session (you will implement this)
        predictionService.determineWinnersForGameWeek(gameWeek.getId());
        System.out.println("✅ Post-processing for gameweek " + gameWeek.getId() + " completed.");
    }

    /**
     * Update matches globally without forcing gameweek associations
     * This method finds existing matches and updates them, then updates all affected gameweeks
     * It does NOT create new gameweek links - matches keep their existing gameweek associations
     */
    @Transactional
    public List<Match> updateMatchesGlobally(List<Match> matchUpdates) {
        System.out.println("🌍 Starting global match updates for " + matchUpdates.size() + " matches");

        List<Match> updatedMatches = new ArrayList<>();

        for (Match matchUpdate : matchUpdates) {
            // Find existing match (don't create new ones)
            Match existingMatch = matchRepository.findByHomeTeamAndAwayTeamAndMatchDate(
                    matchUpdate.getHomeTeam(),
                    matchUpdate.getAwayTeam(),
                    matchUpdate.getMatchDate()
            );

            if (existingMatch != null) {
                System.out.println("🔄 Updating existing match: " + existingMatch.getHomeTeam() + " vs " + existingMatch.getAwayTeam());

                // Update match data (preserve existing gameweek associations)
                existingMatch.setMatchDate(matchUpdate.getMatchDate());
                if (matchUpdate.getHomeScore() != null) {
                    existingMatch.setHomeScore(matchUpdate.getHomeScore());
                }
                if (matchUpdate.getAwayScore() != null) {
                    existingMatch.setAwayScore(matchUpdate.getAwayScore());
                }

                // Update status based on scores
                boolean isCompleted = existingMatch.getHomeScore() != null && existingMatch.getAwayScore() != null;
                existingMatch.setFinished(isCompleted);
                existingMatch.setStatus(isCompleted ? MatchStatus.COMPLETED : MatchStatus.SCHEDULED);
                existingMatch.setPredictionDeadline(existingMatch.getMatchDate().minusMinutes(30));

                // Save and add to updated list
                existingMatch = matchRepository.save(existingMatch);
                updatedMatches.add(existingMatch);

                System.out.println("✅ Match updated - Status: " + existingMatch.getStatus() +
                        ", Belongs to " + existingMatch.getGameweeks().size() + " gameweek(s)");
            } else {
                System.out.println("⚠️ Match not found in database: " + matchUpdate.getHomeTeam() + " vs " + matchUpdate.getAwayTeam() +
                        " on " + matchUpdate.getMatchDate());
            }
        }

        // Update status of ALL gameweeks affected by the updated matches
        if (!updatedMatches.isEmpty()) {
            System.out.println("🔄 Updating status for all affected gameweeks...");
            updateAllAffectedGameWeekStatuses(updatedMatches);
        }

        System.out.println("✅ Global match update completed. Updated " + updatedMatches.size() + " matches");
        return updatedMatches;
    }

    /**
     * Import matches to a specific gameweek (creates new matches and links them)
     * Use this when you want to create new matches for a specific gameweek
     */
    @Transactional
    public void importMatchesToGameWeek(Long gameWeekId, List<Match> importedMatches) {
        System.out.println("📥 Importing " + importedMatches.size() + " matches to gameweek " + gameWeekId);

        GameWeek gameWeek = gameWeekRepository.findById(gameWeekId)
                .orElseThrow(() -> new IllegalArgumentException("GameWeek not found"));

        List<Match> updatedMatches = new ArrayList<>();

        for (Match importedMatch : importedMatches) {
            // Use helper method to find or create match (prevents duplicates)
            Match match = findOrCreateMatch(importedMatch);

            // Link to gameweek - allows same match to belong to multiple gameweeks
            // (e.g., Premier League gameweek + Best Of gameweek)
            linkMatchToGameWeek(match, gameWeek);

            // Save the match and add to updated list
            match = matchRepository.save(match);
            updatedMatches.add(match);
        }

        // Recalculate dates for the current gameweek
        recalculateGameWeekDates(gameWeek);

        // Update status of ALL gameweeks affected by the updated matches
        updateAllAffectedGameWeekStatuses(updatedMatches);

        System.out.println("✅ Import completed for gameweek " + gameWeekId);
    }

    @Transactional
    public boolean updateStatusIfComplete(Long gameWeekId) {
        System.out.println("🔍 GameWeekService.updateStatusIfComplete called for gameweek ID: " + gameWeekId);

        GameWeek gameWeek = gameWeekRepository.findById(gameWeekId)
                .orElseThrow(() -> new IllegalArgumentException("GameWeek not found"));

        System.out.println("📊 Current gameweek status: " + gameWeek.getStatus());
        System.out.println("📊 Total matches in gameweek: " + gameWeek.getMatches().size());

        // Filter active matches only
        List<Match> activeMatches = gameWeek.getMatches().stream()
                .filter(Match::isActive)
                .toList();

        long totalActiveMatches = activeMatches.size();
        long completedMatches = activeMatches.stream()
                .filter(match -> match.getStatus() == MatchStatus.COMPLETED)
                .count();

        long inactiveCount = gameWeek.getMatches().stream()
                .filter(match -> !match.isActive())
                .count();

        System.out.println("⚽ Active matches: " + totalActiveMatches);
        System.out.println("✅ Completed active matches: " + completedMatches + "/" + totalActiveMatches);
        if (inactiveCount > 0) {
            System.out.println("⚠️ Skipping " + inactiveCount + " inactive match(es)");
        }

        // Check if all active matches are completed and status not yet FINISHED
        if (completedMatches == totalActiveMatches &&
                gameWeek.getStatus() != GameweekStatus.FINISHED) {

            System.out.println("🎯 All active matches completed! Updating gameweek status to FINISHED");
            gameWeek.setStatus(GameweekStatus.FINISHED);
            gameWeekRepository.save(gameWeek);

            // Post-finish processing
            System.out.println("🏆 Triggering post-finish processing...");

            // 1. Get all participations of this gameweek
            List<SessionParticipation> participations = predictionService.getSessionParticipationsByGameWeek(gameWeekId);

            // 2. Calculate accuracy for each participation
            for (SessionParticipation participation : participations) {
                predictionService.calculatePredictionAccuracy(participation.getId());
            }

            // 3. Determine winners for all sessions in this gameweek
            predictionService.determineWinnersForGameWeek(gameWeekId);

            System.out.println("✅ Post-finish processing completed.");

            return true;

        } else {
            System.out.println("⏳ GameWeek " + gameWeekId + " is not ready to be finished");
            System.out.println("   Reason: " +
                    (completedMatches != totalActiveMatches
                            ? "Not all matches are completed (" + completedMatches + "/" + totalActiveMatches + ")"
                            : "GameWeek already finished or in another status"));
            return false;
        }
    }

    public GameWeek getByCompetitionAndWeek(LeagueTheme competition, int weekNumber) {
        return gameWeekRepository.findByCompetitionAndWeekNumber(competition, weekNumber)
                .orElseThrow(() -> new IllegalArgumentException("GameWeek not found for competition and week"));
    }

    public List<Match> getMatchesByCompetitionAndWeek(LeagueTheme competition, int weekNumber) {
        GameWeek gameWeek = getByCompetitionAndWeek(competition, weekNumber);
        return matchRepository.findByGameweeksId(gameWeek.getId());
    }

    public List<GameWeek> getUpcomingByCompetition(LeagueTheme competition) {
        LocalDateTime now = LocalDateTime.now();
        return gameWeekRepository.findByCompetitionAndJoinDeadlineAfter(competition, now);
    }
}