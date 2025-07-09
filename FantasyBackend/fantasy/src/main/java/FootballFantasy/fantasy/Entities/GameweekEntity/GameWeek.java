package FootballFantasy.fantasy.Entities.GameweekEntity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameWeek {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private int weekNumber;

    @Enumerated(EnumType.STRING)
    private GameweekStatus status;

    private LocalDate startDate;
    private LocalDate endDate;

    // Add deadline for joining the competition
    @Column(nullable = false)
    private LocalDateTime joinDeadline;
    private String description;

    @JsonIgnore
    @OneToMany(mappedBy = "gameweek", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Match> matches;

    @JsonIgnore
    @OneToMany(mappedBy = "gameweek", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CompetitionSession> sessions = new ArrayList<>();

}
