package FootballFantasy.fantasy.Dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MatchSeedDTO {
    private Long id;
    private String homeTeam;
    private String awayTeam;
    private LocalDateTime matchDate;
}
