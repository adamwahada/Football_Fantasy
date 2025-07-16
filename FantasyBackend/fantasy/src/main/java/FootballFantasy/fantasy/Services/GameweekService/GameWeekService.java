package FootballFantasy.fantasy.Services.GameweekService;

import FootballFantasy.fantasy.Entities.GameweekEntity.GameWeek;
import FootballFantasy.fantasy.Entities.GameweekEntity.LeagueTheme;
import FootballFantasy.fantasy.Entities.GameweekEntity.Match;
import FootballFantasy.fantasy.Entities.GameweekEntity.MatchStatus;
import FootballFantasy.fantasy.Repositories.GameweekRepository.GameWeekRepository;
import FootballFantasy.fantasy.Repositories.GameweekRepository.MatchRepository;
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
                .orElseThrow(() -> new IllegalArgumentException("GameWeek non trouvé"));

        gameWeekRepository.delete(gameWeek);
    }

    // ✅ Add a match to a gameweek without overwriting others
    @Transactional
    public Match addMatchToGameWeek(Long gameWeekId, Match match) {
        GameWeek gameWeek = gameWeekRepository.findById(gameWeekId)
                .orElseThrow(() -> new IllegalArgumentException("GameWeek non trouvé"));

        // ✅ Vérifie que la date du match est dans la plage de la GameWeek
        if (match.getMatchDate().isBefore(gameWeek.getStartDate()) || match.getMatchDate().isAfter(gameWeek.getEndDate())) {
            throw new IllegalArgumentException("La date du match doit être comprise entre la date de début et de fin de la GameWeek.");
        }

        // ✅ Définir le délai de prédiction à 30 min avant le match
        match.setPredictionDeadline(match.getMatchDate().minusMinutes(30));

        // ✅ Ajouter la GameWeek si pas déjà associée
        if (!match.getGameweeks().contains(gameWeek)) {
            match.getGameweeks().add(gameWeek);
        }

        // ✅ Ajouter le match à la GameWeek si pas déjà présent
        if (!gameWeek.getMatches().contains(match)) {
            gameWeek.getMatches().add(match);
        }

        matchRepository.save(match);
        gameWeekRepository.save(gameWeek);

        updateJoinDeadlineForGameWeek(gameWeek);
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
        updateJoinDeadlineForGameWeek(gameWeek);
    }
    @Transactional
    public void deleteSpecificMatchesFromGameWeek(Long gameWeekId, List<Long> matchIdsToRemove) {
        GameWeek gameWeek = gameWeekRepository.findById(gameWeekId)
                .orElseThrow(() -> new IllegalArgumentException("GameWeek not found"));

        for (Match match : new ArrayList<>(gameWeek.getMatches())) {
            if (matchIdsToRemove.contains(match.getId())) {
                match.getGameweeks().remove(gameWeek); // unlink match from gameweek
                gameWeek.getMatches().remove(match);   // unlink gameweek from match
                matchRepository.save(match);
            }
        }

        gameWeekRepository.save(gameWeek);
        updateJoinDeadlineForGameWeek(gameWeek);
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

        updateJoinDeadlineForGameWeek(gameWeek);
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
        updateJoinDeadlineForGameWeek(gameWeek);
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

    public void updateJoinDeadlineForGameWeek(GameWeek gameWeek) {
        List<Match> matches = gameWeek.getMatches();
        if (matches.isEmpty()) return;

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

        gameWeekRepository.save(gameWeek);
    }

    public List<GameWeek> getGameWeeksByCompetition(LeagueTheme Competition) {
        return gameWeekRepository.findByCompetition(Competition);
    }

    @Transactional
    public void importMatchesToGameWeek(Long gameWeekId, List<Match> matchesToAdd) {
        GameWeek gameWeek = gameWeekRepository.findById(gameWeekId)
                .orElseThrow(() -> new IllegalArgumentException("GameWeek not found"));

        for (Match match : matchesToAdd) {
            // Add gameweek reference if missing
            if (!match.getGameweeks().contains(gameWeek)) {
                match.getGameweeks().add(gameWeek);
            }

            // Add match to gameweek if missing
            if (!gameWeek.getMatches().contains(match)) {
                gameWeek.getMatches().add(match);
            }

            // Set prediction deadline (optional)
            match.setPredictionDeadline(match.getMatchDate().minusMinutes(30));

            matchRepository.save(match);
        }

        // Update gameweek dates after all matches are added
        updateJoinDeadlineForGameWeek(gameWeek);
        gameWeekRepository.save(gameWeek);
    }

}
