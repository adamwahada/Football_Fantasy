package FootballFantasy.fantasy.Services.GameweekService;

import FootballFantasy.fantasy.Dto.MatchWithIconsDTO;
import FootballFantasy.fantasy.Entities.GameweekEntity.GameweekStatus;
import FootballFantasy.fantasy.Entities.GameweekEntity.*;
import FootballFantasy.fantasy.Events.MatchCompletedEvent;
import FootballFantasy.fantasy.Events.MatchRescheduledEvent;
import FootballFantasy.fantasy.Repositories.GameweekRepository.GameWeekRepository;
import FootballFantasy.fantasy.Repositories.GameweekRepository.MatchRepository;
import FootballFantasy.fantasy.Services.DataService.TeamIconService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GameWeekService {

    @Autowired
    private GameWeekRepository gameWeekRepository;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private PredictionService predictionService;
    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private CompetitionSessionService competitionSessionService;

    @Autowired
    private TeamIconService teamIconService;

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

    // ✅ New method to return Matches with team icons
    public List<MatchWithIconsDTO> getMatchesByGameWeekWithIcons(Long gameWeekId) {
        List<Match> matches = matchRepository.findByGameweeksId(gameWeekId);
        return matches.stream()
                .map(match -> MatchWithIconsDTO.builder()
                        .id(match.getId())
                        .homeTeam(match.getHomeTeam())
                        .awayTeam(match.getAwayTeam())
                        .homeTeamIcon(teamIconService.getTeamIcon(match.getHomeTeam()))
                        .awayTeamIcon(teamIconService.getTeamIcon(match.getAwayTeam()))
                        .matchDate(match.getMatchDate())
                        .homeScore(match.getHomeScore())
                        .awayScore(match.getAwayScore())
                        .active(match.isActive())
                        .finished(match.isFinished())
                        .predictionDeadline(match.getPredictionDeadline())
                        .description(match.getDescription())
                        .build()
                )
                .collect(Collectors.toList());
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
     * ✅ ENHANCED: Helper method to update status of all gameweeks that contain the given matches
     * This triggers AUTOMATIC winner determination when gameweeks complete
     */
    private void updateAllAffectedGameWeekStatuses(List<Match> updatedMatches) {
        System.out.println("🔍 DEBUG: updateAllAffectedGameWeekStatuses called with " + updatedMatches.size() + " matches");

        Set<GameWeek> affectedGameWeeks = new HashSet<>();
        for (Match match : updatedMatches) {
            System.out.println("🔍 DEBUG: Match " + match.getId() + " belongs to " + match.getGameweeks().size() + " gameweeks");
            affectedGameWeeks.addAll(match.getGameweeks());
        }

        System.out.println("🔍 DEBUG: Found " + affectedGameWeeks.size() + " affected gameweeks");

        for (GameWeek gameWeek : affectedGameWeeks) {
            System.out.println("🔄 DEBUG: Processing gameweek ID: " + gameWeek.getId() + " (current status: " + gameWeek.getStatus() + ")");

            List<Match> gameweekMatches = matchRepository.findByGameweeksId(gameWeek.getId());
            System.out.println("🔍 DEBUG: Found " + gameweekMatches.size() + " matches for gameweek " + gameWeek.getId());

            // Filter active matches only
            List<Match> activeMatches = gameweekMatches.stream()
                    .filter(Match::isActive)
                    .toList();

            long totalActiveMatches = activeMatches.size();
            long completedMatches = activeMatches.stream()
                    .filter(match -> {
                        boolean isCompleted = match.getStatus() == MatchStatus.COMPLETED;
                        System.out.println("🔍 DEBUG: Match " + match.getId() + " (" + match.getHomeTeam() + " vs " + match.getAwayTeam() + ") - Status: " + match.getStatus() + ", Completed: " + isCompleted);
                        return isCompleted;
                    })
                    .count();

            System.out.println("⚽ DEBUG: Active matches: " + totalActiveMatches + ", Completed: " + completedMatches);

            GameweekStatus newStatus;

            if (completedMatches == 0) {
                newStatus = GameweekStatus.UPCOMING;
                System.out.println("📊 DEBUG: Setting to UPCOMING (no completed matches)");
            } else if (completedMatches < totalActiveMatches) {
                newStatus = GameweekStatus.ONGOING;
                System.out.println("📊 DEBUG: Setting to ONGOING (some completed matches)");
            } else {
                newStatus = GameweekStatus.FINISHED;
                System.out.println("📊 DEBUG: Setting to FINISHED (all completed matches)");
            }

            if (gameWeek.getStatus() != newStatus) {
                GameweekStatus oldStatus = gameWeek.getStatus();
                System.out.println("📊 DEBUG: Updating gameweek " + gameWeek.getId() + " status from " + oldStatus + " to " + newStatus);

                gameWeek.setStatus(newStatus);
                GameWeek savedGameWeek = gameWeekRepository.save(gameWeek);
                System.out.println("✅ DEBUG: Gameweek saved with status: " + savedGameWeek.getStatus());

                // Only trigger post-processing when transitioning TO FINISHED
                if (newStatus == GameweekStatus.FINISHED) {
                    System.out.println("🏆 DEBUG: Triggering post-finish processing...");
                    triggerPostGameWeekFinishedActions(gameWeek);
                }
            } else {
                System.out.println("✓ DEBUG: Gameweek " + gameWeek.getId() + " status unchanged (" + newStatus + ")");
            }
        }

        System.out.println("✅ DEBUG: updateAllAffectedGameWeekStatuses completed");
    }    /**
     * ✅ STREAMLINED: Remove duplicate logic - CompetitionSessionService handles everything
     */
    private void triggerPostGameWeekFinishedActions(GameWeek gameWeek) {
        System.out.println("🏆 GameWeek " + gameWeek.getId() + " finished. Triggering AUTOMATIC post-processing...");

        // ✅ SINGLE CALL: This handles everything (accuracy calculation + winner determination + prize distribution)
        competitionSessionService.determineWinnersForCompletedGameWeek(gameWeek.getId());

        System.out.println("✅ AUTOMATIC post-processing for gameweek " + gameWeek.getId() + " completed.");
    }

    /**
     * ✅ SIMPLIFIED: Remove duplicate logic from updateStatusIfComplete
     */
    @Transactional
    public boolean updateStatusIfComplete(Long gameWeekId) {
        GameWeek gameWeek = gameWeekRepository.findById(gameWeekId)
                .orElseThrow(() -> new IllegalArgumentException("GameWeek not found"));

        List<Match> activeMatches = gameWeek.getMatches().stream()
                .filter(Match::isActive)
                .toList();

        long totalActiveMatches = activeMatches.size();
        long completedMatches = activeMatches.stream()
                .filter(match -> match.getStatus() == MatchStatus.COMPLETED)
                .count();

        GameweekStatus oldStatus = gameWeek.getStatus();
        GameweekStatus newStatus;

        if (totalActiveMatches == 0) {
            // No active matches → UPCOMING or your default
            newStatus = GameweekStatus.UPCOMING;
        } else if (completedMatches == totalActiveMatches) {
            newStatus = GameweekStatus.FINISHED;
        } else if (completedMatches > 0) {
            newStatus = GameweekStatus.ONGOING;
        } else {
            newStatus = GameweekStatus.UPCOMING;
        }

        if (newStatus != oldStatus) {
            gameWeek.setStatus(newStatus);
            gameWeekRepository.save(gameWeek);

            if (newStatus == GameweekStatus.FINISHED) {
                triggerPostGameWeekFinishedActions(gameWeek);
            }

            System.out.println("🔄 Gameweek " + gameWeekId + " status changed from " + oldStatus + " to " + newStatus);
            return true;
        } else {
            System.out.println("✓ Gameweek " + gameWeekId + " status unchanged (" + oldStatus + ")");
            return false;
        }
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

        // ✅ THIS IS THE FIX - FORCE UPDATE GAMEWEEK STATUSES
        if (!updatedMatches.isEmpty()) {
            System.out.println("🔄 Updating status for all affected gameweeks...");

            // Get all affected gameweeks
            Set<GameWeek> affectedGameWeeks = new HashSet<>();
            for (Match match : updatedMatches) {
                affectedGameWeeks.addAll(match.getGameweeks());
            }

            System.out.println("📊 Found " + affectedGameWeeks.size() + " affected gameweeks");

            // Update each gameweek status
            for (GameWeek gameWeek : affectedGameWeeks) {
                System.out.println("🔄 Processing gameweek ID: " + gameWeek.getId() + " (current status: " + gameWeek.getStatus() + ")");

                // Get all matches for this gameweek
                List<Match> gameweekMatches = matchRepository.findByGameweeksId(gameWeek.getId());

                // Filter active matches
                List<Match> activeMatches = gameweekMatches.stream()
                        .filter(Match::isActive)
                        .toList();

                long totalActiveMatches = activeMatches.size();
                long completedMatches = activeMatches.stream()
                        .filter(match -> match.getStatus() == MatchStatus.COMPLETED)
                        .count();

                System.out.println("⚽ Active matches: " + totalActiveMatches + ", Completed: " + completedMatches);

                // Determine new status
                GameweekStatus newStatus;
                if (completedMatches == 0) {
                    newStatus = GameweekStatus.UPCOMING;
                } else if (completedMatches < totalActiveMatches) {
                    newStatus = GameweekStatus.ONGOING;
                } else {
                    newStatus = GameweekStatus.FINISHED;
                }

                // Update if changed
                if (gameWeek.getStatus() != newStatus) {
                    GameweekStatus oldStatus = gameWeek.getStatus();
                    gameWeek.setStatus(newStatus);
                    gameWeekRepository.save(gameWeek);

                    System.out.println("📊 ✅ Gameweek " + gameWeek.getId() + " status updated from " + oldStatus + " to " + newStatus);

                    // Trigger post-processing if finished
                    if (newStatus == GameweekStatus.FINISHED) {
                        System.out.println("🏆 Triggering post-finish processing...");
                        triggerPostGameWeekFinishedActions(gameWeek);
                    }
                } else {
                    System.out.println("✓ Gameweek " + gameWeek.getId() + " status unchanged (" + newStatus + ")");
                }
            }
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

    public void setTiebreakersForGameWeek(Long gameweekId, List<Long> matchIds) {
        if (matchIds.size() != 3) {
            throw new IllegalArgumentException("Exactly 3 tiebreaker match IDs must be provided");
        }

        GameWeek gameWeek = gameWeekRepository.findById(gameweekId)
                .orElseThrow(() -> new IllegalArgumentException("GameWeek not found"));

        // Fetch all match IDs in the gameweek
        List<Match> gameweekMatches = matchRepository.findByGameweeksId(gameweekId);
        Set<Long> validMatchIds = gameweekMatches.stream()
                .map(Match::getId)
                .collect(Collectors.toSet());

        // Validate the submitted matchIds
        for (Long id : matchIds) {
            if (!validMatchIds.contains(id)) {
                throw new IllegalArgumentException("Match ID " + id + " does not belong to GameWeek " + gameweekId);
            }
        }

        gameWeek.setTiebreakerMatchIdList(matchIds);
        gameWeekRepository.save(gameWeek);
    }
    public void updateTiebreakers(Long gameweekId, List<Long> newMatchIds) {
        GameWeek gameWeek = gameWeekRepository.findById(gameweekId)
                .orElseThrow(() -> new IllegalArgumentException("GameWeek not found"));

        // If empty/null input, remove tiebreakers
        if (newMatchIds == null || newMatchIds.isEmpty()) {
            gameWeek.setTiebreakerMatchIdList(Collections.emptyList());
            gameWeekRepository.save(gameWeek);
            return;
        }

        // Validate size
        if (newMatchIds.size() != 3) {
            throw new IllegalArgumentException("Exactly 3 tiebreaker match IDs must be provided");
        }

        // Validate that the matches belong to the GameWeek
        List<Match> matches = matchRepository.findByGameweeksId(gameweekId);
        Set<Long> validMatchIds = matches.stream().map(Match::getId).collect(Collectors.toSet());

        for (Long id : newMatchIds) {
            if (!validMatchIds.contains(id)) {
                throw new IllegalArgumentException("Match ID " + id + " does not belong to GameWeek " + gameweekId);
            }
        }

        gameWeek.setTiebreakerMatchIdList(newMatchIds);
        gameWeekRepository.save(gameWeek);
    }


    public List<Match> getTiebreakerMatches(Long gameweekId) {
        GameWeek gameWeek = gameWeekRepository.findById(gameweekId)
                .orElseThrow(() -> new IllegalArgumentException("GameWeek not found"));

        List<Long> ids = gameWeek.getTiebreakerMatchIdList();
        return matchRepository.findAllById(ids);
    }

    // ✅ NEW: Scheduled task to handle expired sessions (runs every 5 minutes)
    @Scheduled(fixedRate = 300000) // 5 minutes
    @Transactional
    public void processExpiredSessions() {
        System.out.println("🔍 Checking for expired sessions that need refunds...");

        LocalDateTime now = LocalDateTime.now();

        // Find sessions that are OPEN but past their join deadline
        // Note: You'll need to add this method to CompetitionSessionRepository
        try {
            List<CompetitionSession> expiredSessions = competitionSessionService
                    .findExpiredOpenSessions(now);

            for (CompetitionSession session : expiredSessions) {
                try {
                    System.out.println("⏰ Processing expired session: " + session.getId());

                    // If it's a one-vs-one with only 1 participant, refund them
                    if (session.getSessionType() == SessionType.ONE_VS_ONE &&
                            session.getCurrentParticipants() == 1) {

                        competitionSessionService.determineWinner(session.getId()); // This will trigger refund logic
                        System.out.println("✅ Refund processed for expired one-vs-one session: " + session.getId());

                    } else if (session.getCurrentParticipants() == 1) {
                        // Any session with only 1 participant gets refunded
                        competitionSessionService.determineWinner(session.getId());
                        System.out.println("✅ Refund processed for expired single-participant session: " + session.getId());

                    } else if (session.getCurrentParticipants() == 0) {
                        // Mark as cancelled if no participants
                        competitionSessionService.cancelEmptySession(session.getId());
                        System.out.println("❌ Cancelled expired session with no participants: " + session.getId());
                    }

                } catch (Exception e) {
                    System.err.println("❌ Error processing expired session " + session.getId() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error in processExpiredSessions: " + e.getMessage());
        }
    }

    public boolean updateStatusIfRescheduled(Long gameweekId) {
        System.out.println("🔁 GameWeekService.updateStatusIfRescheduled called for gameweek ID: " + gameweekId);

        GameWeek gameWeek = gameWeekRepository.findById(gameweekId)
                .orElseThrow(() -> new IllegalArgumentException("Gameweek not found"));

        System.out.println("📊 Current gameweek status: " + gameWeek.getStatus());

        // Get matches using the same method as other functions for consistency
        List<Match> allMatches = matchRepository.findByGameweeksId(gameweekId);

        // Filter active matches only (same logic as updateStatusIfComplete)
        List<Match> activeMatches = allMatches.stream()
                .filter(Match::isActive)
                .toList();

        long totalActiveMatches = activeMatches.size();
        long completedMatches = activeMatches.stream()
                .filter(match -> match.getStatus() == MatchStatus.COMPLETED) // Use MatchStatus for consistency
                .count();

        long inactiveCount = allMatches.stream()
                .filter(match -> !match.isActive())
                .count();

        System.out.println("⚽ Total matches in gameweek: " + allMatches.size());
        System.out.println("⚽ Active matches: " + totalActiveMatches);
        System.out.println("✅ Completed active matches: " + completedMatches + "/" + totalActiveMatches);
        if (inactiveCount > 0) {
            System.out.println("⚠️ Skipping " + inactiveCount + " inactive match(es)");
        }

        // Determine new status based on completed matches
        GameweekStatus newStatus;

        if (completedMatches == 0) {
            // No matches are completed
            newStatus = GameweekStatus.UPCOMING;
            System.out.println("📊 No matches completed -> UPCOMING");
        } else if (completedMatches < totalActiveMatches) {
            // Some matches are completed, some are not
            newStatus = GameweekStatus.ONGOING;
            System.out.println("📊 Some matches completed -> ONGOING");
        } else {
            // All active matches are completed
            newStatus = GameweekStatus.FINISHED;
            System.out.println("📊 All active matches completed -> FINISHED");
        }

        // Update status if it changed
        if (newStatus != gameWeek.getStatus()) {
            GameweekStatus oldStatus = gameWeek.getStatus();
            gameWeek.setStatus(newStatus);
            gameWeekRepository.save(gameWeek);

            System.out.println("🔄 Gameweek " + gameweekId + " status updated from " + oldStatus + " to " + newStatus);

            // If transitioning TO FINISHED, trigger post-processing
            if (newStatus == GameweekStatus.FINISHED) {
                System.out.println("🏆 Triggering post-finish processing for newly finished gameweek...");
                triggerPostGameWeekFinishedActions(gameWeek);
            }

            return true;
        } else {
            System.out.println("✓ Gameweek " + gameweekId + " status unchanged (" + newStatus + ")");
            return false;
        }
    }
}