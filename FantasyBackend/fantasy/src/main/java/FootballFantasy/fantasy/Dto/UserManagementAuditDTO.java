package FootballFantasy.fantasy.Dto;


import FootballFantasy.fantasy.Entities.AdminEntities.BanCause;
import FootballFantasy.fantasy.Entities.AdminEntities.UserAction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserManagementAuditDTO {

    private Long id;

    // User information
    private Long userId;
    private String username;
    private String email;

    // Admin information
    private Long adminId;
    private String adminUsername;

    // Action details
    private UserAction action;
    private String details;
    private BanCause reason;

    // Timestamp
    private LocalDateTime timestamp;

    // Additional fields for frontend display
    private String formattedDate;
    private Double amount; // For credit/debit operations
    private Integer days;  // For temporary ban duration
}