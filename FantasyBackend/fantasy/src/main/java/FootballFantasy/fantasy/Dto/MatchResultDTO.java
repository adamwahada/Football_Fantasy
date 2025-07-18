package FootballFantasy.fantasy.Dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MatchResultDTO {
    private String homeTeam;
    private String awayTeam;
    private LocalDateTime matchDate;
    private Integer homeScore;
    private Integer awayScore;
}
