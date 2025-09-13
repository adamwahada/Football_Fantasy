package FootballFantasy.fantasy.Entities.GameweekEntities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor

@Table(
        uniqueConstraints = @UniqueConstraint(columnNames = {"weekNumber", "competition"})
)
public class GameWeek {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int weekNumber;

    @Enumerated(EnumType.STRING)
    private GameweekStatus status;

    @Enumerated(EnumType.STRING)
    private LeagueTheme competition;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    // Add deadline for joining the competition
    @Column(nullable = false)
    private LocalDateTime joinDeadline;
    private String description;
    // 🆕 Store the 3 tiebreaker match IDs as comma-separated string
    @Column(name = "tiebreaker_match_ids", length = 100)
    private String tiebreakerMatchIds;

    @JsonIgnore
    @ManyToMany(mappedBy = "gameweeks")
    private List<Match> matches = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "gameweek", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CompetitionSession> sessions = new ArrayList<>();

    @Transient
    public List<Long> getTiebreakerMatchIdList() {
        if (tiebreakerMatchIds == null || tiebreakerMatchIds.isEmpty()) return List.of();
        return Arrays.stream(tiebreakerMatchIds.split(","))
                .map(String::trim)
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }

    @Transient
    public void setTiebreakerMatchIdList(List<Long> ids) {
        this.tiebreakerMatchIds = ids.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

}
