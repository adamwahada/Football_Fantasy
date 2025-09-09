package FootballFantasy.fantasy.Entities.Chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class CreateSupportTicketDTO {
    private SupportType supportType;
    private String subject;
    private String description;
    private TicketPriority priority = TicketPriority.MEDIUM;
}