package FootballFantasy.fantasy.Entities.AdminEntities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_management_audit")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserManagementAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The user affected
    private Long userId;

    // The admin who performed the action
    private Long adminId;

    // Type of action: CREDIT, DEBIT, BAN, UNBAN
    @Enumerated(EnumType.STRING)
    private UserAction action;

    // Optional: previous balance / new balance / ban duration etc.
    @Column(length = 100)
    private String details;

    @Enumerated(EnumType.STRING)
    private BanCause reason;

    // Timestamp of the action
    private LocalDateTime timestamp = LocalDateTime.now();
}
