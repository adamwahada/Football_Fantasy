package FootballFantasy.fantasy.Entities.Chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicketDTO {
    private Long id;
    private String ticketId;
    private String subject;
    private String description;
    private SupportType supportType;
    private SupportStatus status;
    private TicketPriority priority;

    // Informations utilisateur
    private Long userId;
    private String userName;
    private String userEmail;

    // Informations admin
    private Long assignedAdminId;
    private String assignedAdminName;

    // Informations ChatRoom associée
    private String chatRoomId; // UUID de la ChatRoom
    private Long unreadMessagesCount;

    // Dates
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;

    // Méthodes utilitaires
    public boolean isActive() {
        return status == SupportStatus.OPEN || status == SupportStatus.IN_PROGRESS;
    }

    public boolean isClosed() {
        return status == SupportStatus.CLOSED || status == SupportStatus.RESOLVED;
    }

    public String getStatusDisplayName() {
        return switch (status) {
            case OPEN -> "Ouvert";
            case IN_PROGRESS -> "En cours";
            case RESOLVED -> "Résolu";
            case CLOSED -> "Fermé";
        };
    }

    public String getTypeDisplayName() {
        return switch (supportType) {
            case PAYMENT -> "Paiement";
            case TECHNICAL -> "Technique";
            case ACCOUNT -> "Compte";
            case GENERAL -> "Général";
        };
    }

    public String getPriorityDisplayName() {
        return switch (priority) {
            case LOW -> "Basse";
            case MEDIUM -> "Moyenne";
            case HIGH -> "Élevée";
            case URGENT -> "Urgente";
        };
    }
}
