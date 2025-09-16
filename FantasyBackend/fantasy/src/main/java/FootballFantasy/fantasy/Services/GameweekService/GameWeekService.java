package FootballFantasy.fantasy.Services.GameweekService;

import FootballFantasy.fantasy.Dto.MatchWithIconsDTO;
import FootballFantasy.fantasy.Entities.GameweekEntities.GameweekStatus;
import FootballFantasy.fantasy.Entities.GameweekEntities.*;
import FootballFantasy.fantasy.Repositories.GameweekRepositories.GameWeekRepository;
import FootballFantasy.fantasy.Repositories.GameweekRepositories.MatchRepository;
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

    @Transactional
    public void deleteGameWeek(Long gameWeekId) {
        GameWeek gameWeek = gameWeekRepository.findById(gameWeekId)
                .orElseThrow(() -> new RuntimeException("GameWeek not found"));

        // Detach from matches first
        for (Match match : new ArrayList<>(gameWeek.getMatches())) {
            match.getGameweeks().remove(gameWeek);
        }

        gameWeek.getMatches().clear();

        gameWeekRepository.delete(gameWeek);
    }


    @Transactional
    public Match addMatchToGameWeek(Long gameWeekId, Match matchData) {
        GameWeek gameWeek = gameWeekRepository.findById(gameWeekId)
                .orElseThrow(() -> new IllegalArgumentException("GameWeek non trouvé"));

        // Use helper method to find or create match (prevents duplicates)
        Match match = findOrCreateMatch(matchData);

        // Link match to gameweek
        linkMatchToGameWeek(match, gameWeek);

        // ✅ Save the match AND automatically update gameweek
        match = saveAndUpdateGameWeek(match);

        return match;
    }


    // 2. ADD a new flexible method (optional - for creating gameweeks automatically)
    @Transactional
    public Match addMatchToGameweekOrCreate(String competition, Integer weekNumber, Match matchData) {
        // Find or create gameweek
        Optional<GameWeek> existingGameweek = gameWeekRepository.findByCompetitionAndWeekNumber(
                LeagueTheme.valueOf(competition), weekNumber);

        GameWeek gameweek;
        if (existingGameweek.isPresent()) {
            gameweek = existingGameweek.get();
            System.out.println("✅ Found existing gameweek: " + competition + " Week " + weekNumber);
        } else {
            // Create new gameweek with flexible dates
            gameweek = new GameWeek();
            gameweek.setCompetition(LeagueTheme.valueOf(competition));
            gameweek.setWeekNumber(weekNumber);
            gameweek.setStatus(GameweekStatus.UPCOMING);
            gameweek.setDescription("Week " + weekNumber + " - " + competition);

            // Initial dates - will be recalculated when match is added
            gameweek.setStartDate(matchData.getMatchDate());
            gameweek.setEndDate(matchData.getMatchDate().plusHours(2).plusMinutes(30));
            gameweek.setJoinDeadline(matchData.getMatchDate().minusMinutes(30));

            gameweek = gameWeekRepository.save(gameweek);
            System.out.println("✅ Created new flexible gameweek: " + competition + " Week " + weekNumber);
        }

        return addMatchToGameWeek(gameweek.getId(), matchData);
    }

    // ✅ FIX: Only return active matches
    public List<Match> getMatchesByGameWeek(Long gameWeekId) {
        return matchRepository.findByGameweeksIdAndActiveTrue(gameWeekId);
    }

    public List<Match> getAllMatchesByGameWeek(Long gameWeekId) {
        // For admin operations that need to see inactive matches too
        return matchRepository.findByGameweeksId(gameWeekId);
    }

    // ✅ New method to return Matches with team icons
    public List<MatchWithIconsDTO> getMatchesByGameWeekWithIcons(Long gameWeekId) {
        // ✅ FIX: Only get active matches
        List<Match> matches = matchRepository.findByGameweeksIdAndActiveTrue(gameWeekId);
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

            // ✅ Save match AND update gameweek dates/statuses
            saveAndUpdateGameWeek(match);
        }
    }

    @Transactional
    public Match saveAndUpdateGameWeek(Match match) {
        Match saved = matchRepository.save(match);
        updateAllAffectedGameWeekStatuses(List.of(saved));
        return saved;
    }

    public GameWeek getGameweekById(Long id) {
        return gameWeekRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Gameweek not found with id: " + id));
    }

    @Transactional
    public void deleteSpecificMatchesFromGameWeek(Long gameWeekId, List<Long> matchIdsToRemove) {
        GameWeek gameWeek = gameWeekRepository.findById(gameWeekId)
                .orElseThrow(() -> new IllegalArgumentException("GameWeek not found"));

        for (Match match : new ArrayList<>(gameWeek.getMatches())) {
            if (matchIdsToRemove.contains(match.getId())) {
                match.getGameweeks().remove(gameWeek);
                gameWeek.getMatches().remove(match);

                // ✅ Save match AND update gameweek dates/statuses
                saveAndUpdateGameWeek(match);
            }
        }
    }


    @Transactional
    public Match linkExistingMatchToGameWeek(Long gameWeekId, Long matchId) {
        GameWeek gameWeek = gameWeekRepository.findById(gameWeekId)
                .orElseThrow(() -> new IllegalArgumentException("GameWeek not found"));

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found"));

        // Use helper method to link (prevents duplicate linking)
        linkMatchToGameWeek(match, gameWeek);

        // ✅ Save match AND update gameweek dates/statuses
        return saveAndUpdateGameWeek(match);
    }


    @Transactional
    public List<Match> linkMultipleMatchesToGameWeek(Long gameWeekId, List<Long> matchIds) {
        GameWeek gameWeek = gameWeekRepository.findById(gameWeekId)
                .orElseThrow(() -> new IllegalArgumentException("GameWeek not found"));

        List<Match> updatedMatches = new ArrayList<>();

        for (Long matchId : matchIds) {
            Match match = matchRepository.findById(matchId)
                    .orElseThrow(() -> new IllegalArgumentException("Match not found with ID: " + matchId));

            // Link match to gameweek
            linkMatchToGameWeek(match, gameWeek);

            // Save match AND update gameweek
            updatedMatches.add(saveAndUpdateGameWeek(match));
        }

        return updatedMatches;
    }


    public GameWeek getByWeekNumber(int weekNumber) {
        return gameWeekRepository.findByWeekNumber(weekNumber)
                .orElseThrow(() -> new IllegalArgumentException("GameWeek not found for week number " + weekNumber));
    }

    public boolean isGameWeekComplete(Long gameWeekId) {
        List<Match> activeMatches = matchRepository.findByGameweeksIdAndActiveTrue(gameWeekId);

        return activeMatches.stream()
                .allMatch(m -> m.getStatus() == MatchStatus.COMPLETED);
    }

    public void recalculateGameWeekDates(GameWeek gameWeek) {
        // ✅ FIX: Only get active matches for date calculation
        List<Match> activeMatches = gameWeek.getMatches().stream()
                .filter(Match::isActive)
                .toList();

        if (activeMatches.isEmpty()) {
            gameWeek.setStartDate(null);
            gameWeek.setEndDate(null);
            // Default far future deadline when no active matches
            LocalDateTime defaultJoinDeadline = LocalDateTime.of(2026, 1, 1, 1, 0);
            gameWeek.setJoinDeadline(defaultJoinDeadline);
        } else {
            LocalDateTime earliest = activeMatches.stream()
                    .map(Match::getMatchDate)
                    .min(LocalDateTime::compareTo)
                    .orElseThrow();

            LocalDateTime latest = activeMatches.stream()
                    .map(Match::getMatchDate)
                    .max(LocalDateTime::compareTo)
                    .orElseThrow();

            gameWeek.setStartDate(earliest);
            gameWeek.setEndDate(latest.plusHours(2).plusMinutes(30));
            gameWeek.setJoinDeadline(earliest.minusMinutes(30));

            System.out.println("📅 GameWeek " + gameWeek.getId() + " dates recalculated based on " +
                    activeMatches.size() + " ACTIVE matches: " +
                    "Start=" + gameWeek.getStartDate() +
                    ", End=" + gameWeek.getEndDate() +
                    ", JoinDeadline=" + gameWeek.getJoinDeadline());
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
            affectedGameWeeks.addAll(match.getGameweeks());
        }

        System.out.println("🔍 DEBUG: Found " + affectedGameWeeks.size() + " affected gameweeks");

        for (GameWeek gameWeek : affectedGameWeeks) {
            System.out.println("🔄 DEBUG: Processing gameweek ID: " + gameWeek.getId() + " (current status: " + gameWeek.getStatus() + ")");

            // ✅ FIX: Only get active matches for status calculation
            List<Match> activeMatches = matchRepository.findByGameweeksIdAndActiveTrue(gameWeek.getId());
            System.out.println("🔍 DEBUG: Found " + activeMatches.size() + " ACTIVE matches for gameweek " + gameWeek.getId());

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

            if (totalActiveMatches == 0) {
                newStatus = GameweekStatus.UPCOMING; // No active matches
                System.out.println("📊 DEBUG: No active matches -> UPCOMING");
            } else if (completedMatches == 0) {
                newStatus = GameweekStatus.UPCOMING;
                System.out.println("📊 DEBUG: No completed matches -> UPCOMING");
            } else if (completedMatches < totalActiveMatches) {
                newStatus = GameweekStatus.ONGOING;
                System.out.println("📊 DEBUG: Some completed matches -> ONGOING");
            } else {
                newStatus = GameweekStatus.FINISHED;
                System.out.println("📊 DEBUG: All active matches completed -> FINISHED");
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
    }
    /**
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

        // ✅ FIX: Only check active matches
        List<Match> activeMatches = matchRepository.findByGameweeksIdAndActiveTrue(gameWeekId);

        long totalActiveMatches = activeMatches.size();
        long completedMatches = activeMatches.stream()
                .filter(match -> match.getStatus() == MatchStatus.COMPLETED)
                .count();

        GameweekStatus oldStatus = gameWeek.getStatus();
        GameweekStatus newStatus;

        if (totalActiveMatches == 0) {
            newStatus = GameweekStatus.UPCOMING; // No active matches
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

            System.out.println("🔄 Gameweek " + gameWeekId + " status changed from " + oldStatus + " to " + newStatus + " (based on " + totalActiveMatches + " active matches)");
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
            Match existingMatch = matchRepository.findByHomeTeamAndAwayTeamAndMatchDate(
                    matchUpdate.getHomeTeam(),
                    matchUpdate.getAwayTeam(),
                    matchUpdate.getMatchDate()
            );

            if (existingMatch != null) {
                System.out.println("🔄 Updating existing match: " + existingMatch.getHomeTeam() + " vs " + existingMatch.getAwayTeam());

                existingMatch.setMatchDate(matchUpdate.getMatchDate());
                if (matchUpdate.getHomeScore() != null) {
                    existingMatch.setHomeScore(matchUpdate.getHomeScore());
                }
                if (matchUpdate.getAwayScore() != null) {
                    existingMatch.setAwayScore(matchUpdate.getAwayScore());
                }

                boolean isCompleted = existingMatch.getHomeScore() != null && existingMatch.getAwayScore() != null;
                existingMatch.setFinished(isCompleted);
                existingMatch.setStatus(isCompleted ? MatchStatus.COMPLETED : MatchStatus.SCHEDULED);
                existingMatch.setPredictionDeadline(existingMatch.getMatchDate().minusMinutes(30));

                existingMatch = matchRepository.save(existingMatch);
                updatedMatches.add(existingMatch);

                System.out.println("✅ Match updated - Status: " + existingMatch.getStatus() +
                        ", Active: " + existingMatch.isActive() +
                        ", Belongs to " + existingMatch.getGameweeks().size() + " gameweek(s)");
            } else {
                System.out.println("⚠️ Match not found in database: " + matchUpdate.getHomeTeam() + " vs " + matchUpdate.getAwayTeam() +
                        " on " + matchUpdate.getMatchDate());
            }
        }

        // Update gameweek statuses and recalculate dates for affected gameweeks
        if (!updatedMatches.isEmpty()) {
            System.out.println("🔄 Updating status and dates for all affected gameweeks...");

            Set<GameWeek> affectedGameWeeks = new HashSet<>();
            for (Match match : updatedMatches) {
                affectedGameWeeks.addAll(match.getGameweeks());
            }

            System.out.println("📊 Found " + affectedGameWeeks.size() + " affected gameweeks");

            for (GameWeek gameWeek : affectedGameWeeks) {
                // ✅ FIX: Recalculate dates based on active matches only
                recalculateGameWeekDates(gameWeek);

                // Update status based on active matches only
                List<Match> activeMatches = matchRepository.findByGameweeksIdAndActiveTrue(gameWeek.getId());

                long totalActiveMatches = activeMatches.size();
                long completedMatches = activeMatches.stream()
                        .filter(match -> match.getStatus() == MatchStatus.COMPLETED)
                        .count();

                GameweekStatus newStatus;
                if (totalActiveMatches == 0) {
                    newStatus = GameweekStatus.UPCOMING; // No active matches
                } else if (completedMatches == 0) {
                    newStatus = GameweekStatus.UPCOMING;
                } else if (completedMatches < totalActiveMatches) {
                    newStatus = GameweekStatus.ONGOING;
                } else {
                    newStatus = GameweekStatus.FINISHED;
                }

                if (gameWeek.getStatus() != newStatus) {
                    GameweekStatus oldStatus = gameWeek.getStatus();
                    gameWeek.setStatus(newStatus);
                    gameWeekRepository.save(gameWeek);

                    System.out.println("📊 ✅ Gameweek " + gameWeek.getId() + " status updated from " + oldStatus + " to " + newStatus + " (based on " + totalActiveMatches + " active matches)");

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

    // 7. ADD helper method to get active match count for a gameweek:
    public long getActiveMatchCount(Long gameweekId) {
        return matchRepository.countActiveMatchesByGameweek(gameweekId);
    }

    // 8. ADD method to get gameweeks that should be visible to users (with active matches):
    public List<GameWeek> getGameweeksWithActiveMatches(LeagueTheme competition) {
        List<GameWeek> allGameweeks = gameWeekRepository.findByCompetition(competition);

        return allGameweeks.stream()
                .filter(gw -> {
                    long activeCount = getActiveMatchCount(gw.getId());
                    return activeCount > 0; // Only include gameweeks with active matches
                })
                .collect(Collectors.toList());
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

        for (Match importedMatch : importedMatches) {
            // Find or create match
            Match match = findOrCreateMatch(importedMatch);

            // Link to gameweek
            linkMatchToGameWeek(match, gameWeek);

            // Save match AND update gameweek (dates & statuses)
            saveAndUpdateGameWeek(match);
        }

        System.out.println("✅ Import completed for gameweek " + gameWeekId);
    }


    public GameWeek getByCompetitionAndWeek(LeagueTheme competition, int weekNumber) {
        return gameWeekRepository.findByCompetitionAndWeekNumber(competition, weekNumber)
                .orElseThrow(() -> new IllegalArgumentException("GameWeek not found for competition and week"));
    }

    public List<Match> getMatchesByCompetitionAndWeek(LeagueTheme competition, int weekNumber) {
        GameWeek gameWeek = getByCompetitionAndWeek(competition, weekNumber);
        // ✅ FIX: Only return active matches
        return matchRepository.findByGameweeksIdAndActiveTrue(gameWeek.getId());
    }

    public List<GameWeek> getUpcomingByCompetition(LeagueTheme competition) {
        LocalDateTime now = LocalDateTime.now();
        return gameWeekRepository.findByCompetitionAndJoinDeadlineAfter(competition, now);
    }
    public List<GameWeek> getAllByCompetition(LeagueTheme competition) {
        return gameWeekRepository.findByCompetition(competition);
    }


    public void setTiebreakersForGameWeek(Long gameweekId, List<Long> matchIds) {
        if (matchIds.size() != 3) {
            throw new IllegalArgumentException("Exactly 3 tiebreaker match IDs must be provided");
        }

        GameWeek gameWeek = gameWeekRepository.findById(gameweekId)
                .orElseThrow(() -> new IllegalArgumentException("GameWeek not found"));

        // ✅ FIX: Fetch only ACTIVE match IDs in the gameweek
        List<Match> activeGameweekMatches = matchRepository.findByGameweeksIdAndActiveTrue(gameweekId);
        Set<Long> validMatchIds = activeGameweekMatches.stream()
                .map(Match::getId)
                .collect(Collectors.toSet());

        // Validate the submitted matchIds
        for (Long id : matchIds) {
            if (!validMatchIds.contains(id)) {
                throw new IllegalArgumentException("Match ID " + id + " does not belong to active matches in GameWeek " + gameweekId);
            }
        }

        gameWeek.setTiebreakerMatchIdList(matchIds);
        // Auto-validate when 3 tiebreakers are configured
        gameWeek.setValidated(true);
        gameWeekRepository.save(gameWeek);
    }

    public void updateTiebreakers(Long gameweekId, List<Long> newMatchIds) {
        GameWeek gameWeek = gameWeekRepository.findById(gameweekId)
                .orElseThrow(() -> new IllegalArgumentException("GameWeek not found"));

        if (newMatchIds == null || newMatchIds.isEmpty()) {
            gameWeek.setTiebreakerMatchIdList(Collections.emptyList());
            gameWeek.setValidated(false);
            gameWeekRepository.save(gameWeek);
            return;
        }

        if (newMatchIds.size() != 3) {
            throw new IllegalArgumentException("Exactly 3 tiebreaker match IDs must be provided");
        }

        // ✅ FIX: Only get active matches for validation
        List<Match> activeMatches = matchRepository.findByGameweeksIdAndActiveTrue(gameweekId);
        Set<Long> validMatchIds = activeMatches.stream().map(Match::getId).collect(Collectors.toSet());

        for (Long id : newMatchIds) {
            if (!validMatchIds.contains(id)) {
                throw new IllegalArgumentException("Match ID " + id + " does not belong to GameWeek " + gameweekId + " or is inactive");
            }
        }

        gameWeek.setTiebreakerMatchIdList(newMatchIds);
        gameWeek.setValidated(newMatchIds.size() == 3);
        gameWeekRepository.save(gameWeek);
    }

    public List<Match> getTiebreakerMatches(Long gameweekId) {
        GameWeek gameWeek = gameWeekRepository.findById(gameweekId)
                .orElseThrow(() -> new IllegalArgumentException("GameWeek not found"));

        List<Long> ids = gameWeek.getTiebreakerMatchIdList();
        if (ids.isEmpty()) {
            return List.of();
        }

        // ✅ FIX: Only return active tiebreaker matches
        return matchRepository.findAllById(ids).stream()
                .filter(Match::isActive)
                .collect(Collectors.toList());
    }
    @Transactional
    public void validateTiebreakersAreActive(Long gameweekId) {
        GameWeek gameWeek = gameWeekRepository.findById(gameweekId)
                .orElseThrow(() -> new IllegalArgumentException("GameWeek not found"));

        List<Long> tiebreakerIds = gameWeek.getTiebreakerMatchIdList();
        if (tiebreakerIds.isEmpty()) {
            return;
        }

        // Check if all tiebreaker matches are still active
        List<Match> tiebreakerMatches = matchRepository.findAllById(tiebreakerIds);
        List<Long> inactiveIds = tiebreakerMatches.stream()
                .filter(match -> !match.isActive())
                .map(Match::getId)
                .collect(Collectors.toList());

        if (!inactiveIds.isEmpty()) {
            // Remove inactive matches from tiebreakers
            List<Long> activeIds = tiebreakerMatches.stream()
                    .filter(Match::isActive)
                    .map(Match::getId)
                    .collect(Collectors.toList());

            gameWeek.setTiebreakerMatchIdList(activeIds);
            gameWeek.setValidated(activeIds.size() == 3);
            gameWeekRepository.save(gameWeek);

            System.out.println("Removed inactive tiebreaker matches: " + inactiveIds + " from gameweek " + gameweekId);
        }
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

        // ✅ FIX: Only get active matches for status calculation
        List<Match> activeMatches = matchRepository.findByGameweeksIdAndActiveTrue(gameweekId);

        long totalActiveMatches = activeMatches.size();
        long completedMatches = activeMatches.stream()
                .filter(match -> match.getStatus() == MatchStatus.COMPLETED)
                .count();

        System.out.println("⚽ Active matches: " + totalActiveMatches);
        System.out.println("✅ Completed active matches: " + completedMatches + "/" + totalActiveMatches);

        // Determine new status based on completed active matches
        GameweekStatus newStatus;

        if (totalActiveMatches == 0) {
            newStatus = GameweekStatus.UPCOMING; // No active matches
            System.out.println("📊 No active matches -> UPCOMING");
        } else if (completedMatches == 0) {
            newStatus = GameweekStatus.UPCOMING;
            System.out.println("📊 No completed matches -> UPCOMING");
        } else if (completedMatches < totalActiveMatches) {
            newStatus = GameweekStatus.ONGOING;
            System.out.println("📊 Some matches completed -> ONGOING");
        } else {
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
    @Transactional
    public void removeMatchesFromTiebreakers(Long gameweekId, List<Long> matchIdsToRemove) {
        GameWeek gameWeek = gameWeekRepository.findById(gameweekId)
                .orElseThrow(() -> new IllegalArgumentException("GameWeek not found"));

        List<Long> currentTiebreakers = gameWeek.getTiebreakerMatchIdList();
        if (currentTiebreakers == null || currentTiebreakers.isEmpty()) {
            return;
        }

        // Récupérer une copie modifiable au cas où
        List<Long> modifiableTiebreakers = new ArrayList<>(currentTiebreakers);

        System.out.println("Avant suppression: " + modifiableTiebreakers);
        boolean modified = modifiableTiebreakers.removeAll(matchIdsToRemove);
        System.out.println("Après suppression: " + modifiableTiebreakers);

        if (modified) {
            gameWeek.setTiebreakerMatchIdList(modifiableTiebreakers);
            // Maintain validation based on count (3 => true, else false)
            gameWeek.setValidated(modifiableTiebreakers.size() == 3);
            gameWeekRepository.save(gameWeek);
            System.out.println("Tiebreakers mis à jour avec succès.");
        }
    }
    @Transactional
    public GameWeek validateGameWeek(Long gameWeekId) {
        GameWeek gameWeek = gameWeekRepository.findById(gameWeekId)
                .orElseThrow(() -> new IllegalArgumentException("GameWeek not found"));

        gameWeek.setValidated(true);
        return gameWeekRepository.save(gameWeek);
    }

    @Transactional
    public GameWeek unvalidateGameWeek(Long gameWeekId) {
        GameWeek gameWeek = gameWeekRepository.findById(gameWeekId)
                .orElseThrow(() -> new IllegalArgumentException("GameWeek not found"));

        gameWeek.setValidated(false);
        return gameWeekRepository.save(gameWeek);
    }
    public List<GameWeek> getUpcomingValidatedByCompetition(LeagueTheme competition) {
        LocalDateTime now = LocalDateTime.now();
        return gameWeekRepository
                .findByCompetitionAndJoinDeadlineAfterAndValidatedTrue(competition, now);
    }

}