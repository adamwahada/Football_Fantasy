package FootballFantasy.fantasy.Entities.GameweekEntity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @JsonIgnore
    @ManyToMany(mappedBy = "gameweeks")
    private List<Match> matches = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "gameweek", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CompetitionSession> sessions = new ArrayList<>();

}
