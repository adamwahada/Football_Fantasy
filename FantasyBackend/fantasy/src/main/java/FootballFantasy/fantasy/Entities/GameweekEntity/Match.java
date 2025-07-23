package FootballFantasy.fantasy.Entities.GameweekEntity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "football_match")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Match {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String homeTeam;
    private String awayTeam;

    private LocalDateTime matchDate;

    private Integer homeScore;
    private Integer awayScore;

    private LocalDateTime predictionDeadline;

    private boolean finished;
    private String description;
    private boolean active = true;

    @Enumerated(EnumType.STRING)
    private MatchStatus status;

    @JsonIgnore
    @ManyToMany
    @JoinTable(
            name = "match_gameweek",
            joinColumns = @JoinColumn(name = "match_id"),
            inverseJoinColumns = @JoinColumn(name = "gameweek_id")
    )
    private List<GameWeek> gameweeks = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Prediction> predictions;
}
