package FootballFantasy.fantasy.Entities.Chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSupportTicketDTO {
    private SupportStatus status;
    private TicketPriority priority;
    private Long assignedAdminId;
}