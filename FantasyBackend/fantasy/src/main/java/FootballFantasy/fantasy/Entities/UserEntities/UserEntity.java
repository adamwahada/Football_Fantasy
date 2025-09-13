package FootballFantasy.fantasy.Entities.UserEntities;

import FootballFantasy.fantasy.Entities.GameweekEntities.SessionParticipation;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "keycloak_id", unique = true)
    private String keycloakId;

    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String country;
    private String address;
    private String postalNumber;
    private LocalDate birthDate;
    private String referralCode;
    private boolean termsAccepted;

    private boolean active = true;
    // Optional field for temporary bans
    private LocalDateTime bannedUntil;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SessionParticipation> sessionParticipations = new ArrayList<>();

    public boolean isBanned() {
        return !active || (bannedUntil != null && bannedUntil.isAfter(LocalDateTime.now()));
    }

}
