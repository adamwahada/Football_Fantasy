package FootballFantasy.fantasy.Services.DataService;

import FootballFantasy.fantasy.Dto.MatchSeedDTO;
import FootballFantasy.fantasy.Entities.GameweekEntity.GameWeek;
import FootballFantasy.fantasy.Entities.GameweekEntity.Match;
import FootballFantasy.fantasy.Entities.GameweekEntity.MatchStatus;
import FootballFantasy.fantasy.Repositories.GameweekRepository.GameWeekRepository;
import FootballFantasy.fantasy.Repositories.GameweekRepository.MatchRepository;
import FootballFantasy.fantasy.Services.GameweekService.GameWeekService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class MatchSeederService {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MatchRepository matchRepository;

    @Autowired
    private GameWeekRepository gameWeekRepository;

    @Transactional
    public void seedGameWeekByNumber(int weekNumber) throws IOException {
        GameWeek gameWeek = gameWeekRepository.findByWeekNumber(weekNumber)
                .orElseThrow(() -> new IllegalArgumentException("GameWeek not found for week " + weekNumber));

        if (!gameWeek.getMatches().isEmpty()) {
            throw new IllegalStateException("GameWeek " + weekNumber + " already has matches");
        }

        String fileName = "data/premier-league-gw" + weekNumber + ".json";
        InputStream inputStream = new ClassPathResource(fileName).getInputStream();
        List<MatchSeedDTO> seedMatches = Arrays.asList(objectMapper.readValue(inputStream, MatchSeedDTO[].class));

        for (MatchSeedDTO dto : seedMatches) {
            Match match = new Match();
            match.setHomeTeam(dto.getHomeTeam());
            match.setAwayTeam(dto.getAwayTeam());
            match.setMatchDate(dto.getMatchDate());
            match.setHomeScore(0);
            match.setAwayScore(0);
            match.setFinished(false);
            match.setStatus(MatchStatus.SCHEDULED);
            match.setPredictionDeadline(dto.getMatchDate().minusMinutes(30));
            match.getGameweeks().add(gameWeek);
            matchRepository.save(match);
            gameWeek.getMatches().add(match);
        }

        // Update gameweek startDate, endDate, and joinDeadline
        List<Match> allMatches = gameWeek.getMatches();

        LocalDateTime earliest = allMatches.stream()
                .map(Match::getMatchDate)
                .min(LocalDateTime::compareTo)
                .orElseThrow();

        LocalDateTime latest = allMatches.stream()
                .map(Match::getMatchDate)
                .max(LocalDateTime::compareTo)
                .orElseThrow();

        gameWeek.setStartDate(earliest);
        gameWeek.setEndDate(latest.plusHours(2).plusMinutes(30));
        gameWeek.setJoinDeadline(earliest.minusMinutes(30));

        gameWeekRepository.save(gameWeek);
    }

}
