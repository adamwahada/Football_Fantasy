package FootballFantasy.fantasy.Dto;

import FootballFantasy.fantasy.Entities.GameweekEntities.LeagueTheme;
import FootballFantasy.fantasy.Entities.GameweekEntities.Match;
import FootballFantasy.fantasy.Entities.GameweekEntities.SessionType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class GameweekPreviewDTO {
    private Long gameweekId;
    private String gameweekName;
    private LeagueTheme competition;
    private SessionType sessionType;
    private BigDecimal buyInAmount;
    private Integer maxParticipants;
    private LocalDateTime joinDeadline;
    private List<Match> matches;
    private List<Long> tiebreakerMatchIds;
    private boolean isPrivate;
    private String sessionDescription;
    private String accessKey;
    private String keycloakId;
}
