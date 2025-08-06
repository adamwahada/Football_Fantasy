package FootballFantasy.fantasy.Dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MatchWithIconsDTO {
    private Long id;
    private String homeTeam;
    private String awayTeam;
    private String homeTeamIcon;
    private String awayTeamIcon;
    private LocalDateTime matchDate;
    private Integer homeScore;
    private Integer awayScore;
    private boolean active;
    private boolean finished;
    private LocalDateTime predictionDeadline;
    private String description;
}