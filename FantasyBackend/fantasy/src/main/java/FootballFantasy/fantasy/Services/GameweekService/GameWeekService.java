package FootballFantasy.fantasy.Services.GameweekService;

import FootballFantasy.fantasy.Entities.GameweekEntity.GameweekStatus;
import FootballFantasy.fantasy.Entities.GameweekEntity.*;
import FootballFantasy.fantasy.Repositories.GameweekRepository.GameWeekRepository;
import FootballFantasy.fantasy.Repositories.GameweekRepository.MatchRepository;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class GameWeekService {

    @Autowired
    private GameWeekRepository gameWeekRepository;

    @Autowired
    private MatchRepository matchRepository;

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
    public Match addMatchToGameWeek(Long gameWeekId, Match match) {
        GameWeek gameWeek = gameWeekRepository.findById(gameWeekId)
                .orElseThrow(() -> new IllegalArgumentException("GameWeek non trouvé"));

        if (match.getMatchDate().isBefore(gameWeek.getStartDate()) || match.getMatchDate().isAfter(gameWeek.getEndDate())) {
            throw new IllegalArgumentException("La date du match doit être comprise entre la date de début et de fin de la GameWeek.");
        }

        match.setPredictionDeadline(match.getMatchDate().minusMinutes(30));

        if (!match.getGameweeks().contains(gameWeek)) {
            match.getGameweeks().add(gameWeek);
        }

        if (!gameWeek.getMatches().contains(match)) {
            gameWeek.getMatches().add(match);
        }

        matchRepository.save(match);
        gameWeekRepository.save(gameWeek);

        recalculateGameWeekDates(gameWeek);
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

        if (!match.getGameweeks().contains(gameWeek)) {
            match.getGameweeks().add(gameWeek);
        }

        if (!gameWeek.getMatches().contains(match)) {
            gameWeek.getMatches().add(match);
        }

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

            if (!match.getGameweeks().contains(gameWeek)) {
                match.getGameweeks().add(gameWeek);
            }

            if (!gameWeek.getMatches().contains(match)) {
                gameWeek.getMatches().add(match);
            }

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
        return matches.stream().allMatch(m -> m.getStatus() == MatchStatus.COMPLETED);
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

    public List<GameWeek> getGameWeeksByCompetition(LeagueTheme Competition) {
        return gameWeekRepository.findByCompetition(Competition);
    }

    @Transactional
    public void importMatchesToGameWeek(Long gameWeekId, List<Match> importedMatches) {
        GameWeek gameWeek = gameWeekRepository.findById(gameWeekId)
                .orElseThrow(() -> new IllegalArgumentException("GameWeek not found"));

        for (Match importedMatch : importedMatches) {
            Match existingMatch = matchRepository.findByHomeTeamAndAwayTeamAndMatchDate(
                    importedMatch.getHomeTeam(),
                    importedMatch.getAwayTeam(),
                    importedMatch.getMatchDate()
            );

            if (existingMatch != null) {
                existingMatch.setMatchDate(importedMatch.getMatchDate());
                existingMatch.setHomeScore(importedMatch.getHomeScore());
                existingMatch.setAwayScore(importedMatch.getAwayScore());

                boolean isCompleted = importedMatch.getHomeScore() != null && importedMatch.getAwayScore() != null;
                existingMatch.setFinished(isCompleted);
                existingMatch.setStatus(isCompleted ? MatchStatus.COMPLETED : MatchStatus.SCHEDULED);

                existingMatch.setPredictionDeadline(importedMatch.getMatchDate().minusMinutes(30));

                if (!existingMatch.getGameweeks().contains(gameWeek)) {
                    existingMatch.getGameweeks().add(gameWeek);
                }
                matchRepository.save(existingMatch);
            } else {
                importedMatch.setFinished(importedMatch.getHomeScore() != null && importedMatch.getAwayScore() != null);
                importedMatch.setPredictionDeadline(importedMatch.getMatchDate().minusMinutes(30));
                importedMatch.getGameweeks().add(gameWeek);
                matchRepository.save(importedMatch);
                gameWeek.getMatches().add(importedMatch);
            }
        }

        // ✅ Recalculate status after matches updated using fresh DB query
        List<Match> updatedMatches = matchRepository.findByGameweeksId(gameWeekId);
        boolean allCompleted = updatedMatches.stream()
                .allMatch(m -> m.getStatus() == MatchStatus.COMPLETED);

        gameWeek.setStatus(allCompleted ? GameweekStatus.FINISHED : GameweekStatus.ONGOING);

        recalculateGameWeekDates(gameWeek);
        gameWeekRepository.save(gameWeek);
    }

    @Transactional
    public boolean updateStatusIfComplete(Long gameWeekId) {
        System.out.println("🔍 GameWeekService.updateStatusIfComplete called for gameweek ID: " + gameWeekId);

        GameWeek gameWeek = gameWeekRepository.findById(gameWeekId)
                .orElseThrow(() -> new IllegalArgumentException("GameWeek not found"));

        System.out.println("📊 Current gameweek status: " + gameWeek.getStatus());
        System.out.println("📊 Total matches in gameweek: " + gameWeek.getMatches().size());

        // Check if all matches are completed
        long completedMatches = gameWeek.getMatches().stream()
                .filter(match -> match.getStatus() == MatchStatus.COMPLETED)
                .count();

        System.out.println("✅ Completed matches: " + completedMatches + "/" + gameWeek.getMatches().size());

        // Check if all matches are completed and gameweek is not already finished
        if (completedMatches == gameWeek.getMatches().size() &&
                gameWeek.getStatus() != GameweekStatus.FINISHED) {

            System.out.println("🎯 All matches completed! Updating gameweek status to FINISHED");
            gameWeek.setStatus(GameweekStatus.FINISHED);
            gameWeekRepository.save(gameWeek);

            System.out.println("✅ GameWeek " + gameWeekId + " status updated to FINISHED");
            return true;
        } else {
            System.out.println("⏳ GameWeek " + gameWeekId + " is not ready to be finished");
            System.out.println("   Reason: " +
                    (completedMatches != gameWeek.getMatches().size() ?
                            "Not all matches completed (" + completedMatches + "/" + gameWeek.getMatches().size() + ")" :
                            "Already finished"));
            return false;
        }
    }

}
