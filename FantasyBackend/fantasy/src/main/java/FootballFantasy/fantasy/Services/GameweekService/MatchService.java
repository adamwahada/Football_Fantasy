package FootballFantasy.fantasy.Services.GameweekService;

import FootballFantasy.fantasy.Entities.GameweekEntity.GameWeek;
import FootballFantasy.fantasy.Entities.GameweekEntity.Match;
import FootballFantasy.fantasy.Entities.GameweekEntity.MatchStatus;
import FootballFantasy.fantasy.Repositories.GameweekRepository.GameWeekRepository;
import FootballFantasy.fantasy.Repositories.GameweekRepository.MatchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MatchService {

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private GameWeekRepository gameWeekRepository;

    @Autowired
    private GameWeekService gameWeekService;

    @Autowired
    private CompetitionSessionService competitionSessionService;

    public Match createMatch(Match match) {
        // If match has gameweeks, validate each one
        if (match.getGameweeks() != null && !match.getGameweeks().isEmpty()) {
            for (GameWeek gw : match.getGameweeks()) {
                GameWeek gameWeek = gameWeekRepository.findById(gw.getId())
                        .orElseThrow(() -> new IllegalArgumentException("GameWeek non trouvé avec ID: " + gw.getId()));

                // Check that the date is within bounds
                if (match.getMatchDate().isBefore(gameWeek.getStartDate()) || match.getMatchDate().isAfter(gameWeek.getEndDate())) {
                    throw new IllegalArgumentException("La date du match doit être comprise entre la date de début et de fin de la GameWeek.");
                }

                // Bi-directional sync (optional but recommended)
                if (!gameWeek.getMatches().contains(match)) {
                    gameWeek.getMatches().add(match);
                }
            }
        }

        // ✅ Set prediction deadline to 30 minutes before match
        match.setPredictionDeadline(match.getMatchDate().minusMinutes(30));

        return matchRepository.save(match);
    }


    public Match updateMatch(Long matchId, Match updatedMatch) {
        Match existingMatch = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match non trouvé"));

        // Update basic fields
        existingMatch.setHomeTeam(updatedMatch.getHomeTeam());
        existingMatch.setAwayTeam(updatedMatch.getAwayTeam());
        existingMatch.setMatchDate(updatedMatch.getMatchDate());
        existingMatch.setHomeScore(updatedMatch.getHomeScore());
        existingMatch.setAwayScore(updatedMatch.getAwayScore());
        existingMatch.setFinished(updatedMatch.isFinished());
        existingMatch.setDescription(updatedMatch.getDescription());
        existingMatch.setStatus(updatedMatch.getStatus());

        Match savedMatch = matchRepository.save(existingMatch);

        // If the match is completed, check if related gameweeks are now complete
        if (updatedMatch.getStatus() == MatchStatus.COMPLETED) {
            for (GameWeek gameWeek : existingMatch.getGameweeks()) {
                boolean allCompleted = gameWeekService.isGameWeekComplete(gameWeek.getId());
                if (allCompleted) {
                    competitionSessionService.determineWinnersForCompletedGameWeek(gameWeek.getId());
                }
            }
        }

        return savedMatch;
    }

    public void deleteMatch(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match non trouvé"));

        // Remove the match from all associated gameweeks (clean up Many-to-Many)
        for (GameWeek gw : match.getGameweeks()) {
            gw.getMatches().remove(match);
        }

        matchRepository.delete(match);
    }

    public Match getMatchById(Long matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match non trouvé"));
    }

    public List<Match> getAllMatches() {
        return matchRepository.findAll();
    }

    public String getWinner(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match non trouvé"));

        if (!match.isFinished()) {
            return "Le match n'est pas encore terminé.";
        }

        int homeScore = match.getHomeScore() != null ? match.getHomeScore() : 0;
        int awayScore = match.getAwayScore() != null ? match.getAwayScore() : 0;

        if (homeScore > awayScore) {
            return match.getHomeTeam();
        } else if (awayScore > homeScore) {
            return match.getAwayTeam();
        } else {
            return "Match nul";
        }
    }

}
