package FootballFantasy.fantasy.Entities.GameweekEntities;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "session_template")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String templateName; // "1v1 - $10", "Small Group - $20"

    @Column(nullable = false)
    private Boolean isPrivate = false; // Only required for private sessions between friends

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionType sessionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeagueTheme competition;

    @Column(nullable = false)
    private BigDecimal buyInAmount;

    @Column(nullable = false)
    private Integer maxParticipants;

    @Column(nullable = false)
    private Boolean isActive = true; // Admin can enable/disable templates

    @Column(nullable = false)
    private String description; // Optional description
}