package FootballFantasy.fantasy.Services.GameweekService;

import FootballFantasy.fantasy.Entities.GameweekEntity.GameWeek;
import FootballFantasy.fantasy.Entities.GameweekEntity.Match;
import FootballFantasy.fantasy.Entities.GameweekEntity.MatchStatus;
import FootballFantasy.fantasy.Events.MatchCompletedEvent;
import FootballFantasy.fantasy.Repositories.GameweekRepository.GameWeekRepository;
import FootballFantasy.fantasy.Repositories.GameweekRepository.MatchRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
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
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    public Match createMatch(Match match) {
        if (match.getGameweeks() != null) {
            for (GameWeek gw : match.getGameweeks()) {
                GameWeek gameWeek = gameWeekRepository.findById(gw.getId())
                        .orElseThrow(() -> new IllegalArgumentException("GameWeek not found with ID: " + gw.getId()));
                if (match.getMatchDate().isBefore(gameWeek.getStartDate()) ||
                        match.getMatchDate().isAfter(gameWeek.getEndDate())) {
                    throw new IllegalArgumentException("Match date outside GameWeek boundaries");
                }
                if (!gameWeek.getMatches().contains(match)) {
                    gameWeek.getMatches().add(match);
                }
            }
        }
        match.setPredictionDeadline(match.getMatchDate().minusMinutes(30));
        return matchRepository.save(match);
    }

    public Match updateMatch(Long matchId, Match updatedMatch) {
        Match existing = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found"));

        // Store the old status to check if it changed
        MatchStatus oldStatus = existing.getStatus();

        existing.setHomeTeam(updatedMatch.getHomeTeam());
        existing.setAwayTeam(updatedMatch.getAwayTeam());
        existing.setMatchDate(updatedMatch.getMatchDate());
        existing.setHomeScore(updatedMatch.getHomeScore());
        existing.setAwayScore(updatedMatch.getAwayScore());
        existing.setFinished(updatedMatch.isFinished());
        existing.setDescription(updatedMatch.getDescription());
        existing.setStatus(updatedMatch.getStatus());

        Match saved = matchRepository.saveAndFlush(existing);

        // Publish event if match was just completed
        if (oldStatus != MatchStatus.COMPLETED && saved.getStatus() == MatchStatus.COMPLETED) {
            eventPublisher.publishEvent(new MatchCompletedEvent(this, matchId));
        }

        return saved;
    }

    @Transactional
    public void deleteMatch(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found"));
        for (GameWeek gw : match.getGameweeks()) {
            gw.getMatches().remove(match);
            gameWeekService.recalculateGameWeekDates(gw);
        }
        matchRepository.delete(match);
    }

    public Match getMatchById(Long matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new IllegalArgumentException("Match not found"));
    }

    public List<Match> getAllMatches() {
        return matchRepository.findAll();
    }

    public String getWinner(Long matchId) {
        Match match = getMatchById(matchId);
        if (!match.isFinished()) return "Match not finished yet.";
        int home = match.getHomeScore() != null ? match.getHomeScore() : 0;
        int away = match.getAwayScore() != null ? match.getAwayScore() : 0;
        return home > away ? match.getHomeTeam()
                : away > home ? match.getAwayTeam()
                : "Draw";
    }

}