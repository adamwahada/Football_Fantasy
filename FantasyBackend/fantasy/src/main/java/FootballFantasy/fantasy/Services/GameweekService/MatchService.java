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
        if (match.getGameweek() != null && match.getGameweek().getId() != null) {
            GameWeek gameWeek = gameWeekRepository.findById(match.getGameweek().getId())
                    .orElseThrow(() -> new IllegalArgumentException("GameWeek non trouvé"));

            // Vérification que la date est dans l'intervalle de la GameWeek
            if (match.getMatchDate().isBefore(gameWeek.getStartDate()) || match.getMatchDate().isAfter(gameWeek.getEndDate())) {
                throw new IllegalArgumentException("La date du match doit être comprise entre la date de début et de fin de la GameWeek.");
            }

            match.setGameweek(gameWeek);  // associer uniquement si valide
        }

        // sinon : match sans GameWeek → aucun souci
        return matchRepository.save(match);
    }

    public Match updateMatch(Long matchId, Match updatedMatch) {
        Match existingMatch = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match non trouvé"));

        // Update fields normally
        existingMatch.setHomeTeam(updatedMatch.getHomeTeam());
        existingMatch.setAwayTeam(updatedMatch.getAwayTeam());
        existingMatch.setMatchDate(updatedMatch.getMatchDate());
        existingMatch.setHomeScore(updatedMatch.getHomeScore());
        existingMatch.setAwayScore(updatedMatch.getAwayScore());
        existingMatch.setFinished(updatedMatch.isFinished());
        existingMatch.setDescription(updatedMatch.getDescription());
        existingMatch.setStatus(updatedMatch.getStatus());

        Match savedMatch = matchRepository.save(existingMatch);

        // --- New logic for automatic winner determination ---
        if (updatedMatch.getStatus() == MatchStatus.COMPLETED) {
            Long gameWeekId = existingMatch.getGameweek() != null ? existingMatch.getGameweek().getId() : null;
            if (gameWeekId != null) {
                boolean allMatchesCompleted = gameWeekService.isGameWeekComplete(gameWeekId);
                if (allMatchesCompleted) {
                    competitionSessionService.determineWinnersForCompletedGameWeek(gameWeekId);
                }
            }
        }

        return savedMatch;
    }

    public void deleteMatch(Long matchId) {
        matchRepository.deleteById(matchId);
    }

    public Match getMatchById(Long matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match non trouvé"));
    }

    public List<Match> getAllMatches() {
        return matchRepository.findAll();
    }

    //RETOURNE GAGNEUR
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
