package FootballFantasy.fantasy.Entities.GameweekEntity;

import FootballFantasy.fantasy.Entities.UserEntity.UserEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Entity
@Table(name = "competition_session")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompetitionSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sessionName; // e.g., "$10 - 1v1", "$20 - Open Room"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionType sessionType;

    @Column(nullable = false)
    private BigDecimal buyInAmount; // $10, $20, $50

    @Column(nullable = false)
    private Integer maxParticipants; // 2 for 1v1, 5 for small, 10 for medium, null for open

    @Column(name = "access_key", nullable = true, unique = true)
    private String accessKey;  // The key used to join private sessions

    @Column(nullable = false)
    private Integer currentParticipants = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompetitionSessionStatus status = CompetitionSessionStatus.OPEN;

    @Column(nullable = false)
    private LocalDateTime joinDeadline;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // Prize pool calculation
    @Column(nullable = false)
    private BigDecimal totalPrizePool = BigDecimal.ZERO;

    // Winner of this specific session
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private UserEntity winner;

    // Link to gameweek
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gameweek_id", nullable = false)
    private GameWeek gameweek;

    // Participants in this session
    @JsonIgnore
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SessionParticipation> participations = new ArrayList<>();

    // Check if session is full
    public boolean isFull() {
        return maxParticipants != null && currentParticipants >= maxParticipants;
    }

    // Check if session can accept more participants

    public boolean canJoin() {
        return status == CompetitionSessionStatus.OPEN &&
                !isFull() &&
                joinDeadline != null &&
                LocalDateTime.now().isBefore(joinDeadline);
    }
    public boolean isPrivate() {
        return accessKey != null && !accessKey.isEmpty();
    }
}