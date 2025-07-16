package FootballFantasy.fantasy.Dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MatchSeedDTO {
    private String homeTeam;
    private String awayTeam;
    private LocalDateTime matchDate;
}
