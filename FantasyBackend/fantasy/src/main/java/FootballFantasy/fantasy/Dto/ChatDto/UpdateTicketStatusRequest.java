package FootballFantasy.fantasy.Dto.ChatDto;

import FootballFantasy.fantasy.Entities.Chat.SupportStatus;
import FootballFantasy.fantasy.Entities.Chat.TicketPriority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTicketStatusRequest {
    
    @NotNull(message = "Le statut est obligatoire")
    private SupportStatus status;
    
    private TicketPriority priority;
    
    private String adminNote; // Note optionnelle de l'admin lors du changement de statut
    
    // Constructeur simple pour changement de statut uniquement
    public UpdateTicketStatusRequest(SupportStatus status) {
        this.status = status;
    }
    
    // Constructeur avec note
    public UpdateTicketStatusRequest(SupportStatus status, String adminNote) {
        this.status = status;
        this.adminNote = adminNote;
    }
}
