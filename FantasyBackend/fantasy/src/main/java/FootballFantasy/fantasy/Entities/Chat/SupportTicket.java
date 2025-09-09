package FootballFantasy.fantasy.Entities.Chat;

import FootballFantasy.fantasy.Entities.UserEntity.UserEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "support_tickets")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportTicket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 20)
    private String ticketId; // Format: "TICKET-001234"

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "support_type", nullable = false)
    private SupportType supportType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SupportStatus status = SupportStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority")
    private TicketPriority priority = TicketPriority.MEDIUM;

    // Relation avec l'utilisateur qui a créé le ticket
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private UserEntity user;

    // Relation avec l'admin assigné (optionnel)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_admin_id")
    @JsonIgnore
    private UserEntity assignedAdmin;

    // Relation ONE-TO-ONE avec ChatRoom
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "chat_room_id", unique = true)
    @JsonIgnore
    private ChatRoom chatRoom;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    // Méthodes utilitaires
    public void markAsResolved(UserEntity admin) {
        this.status = SupportStatus.RESOLVED;
        this.assignedAdmin = admin;
        this.resolvedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsClosed(UserEntity admin) {
        this.status = SupportStatus.CLOSED;
        this.closedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.assignedAdmin == null) {
            this.assignedAdmin = admin;
        }
    }

    public void assignToAdmin(UserEntity admin) {
        this.assignedAdmin = admin;
        if (this.status == SupportStatus.OPEN) {
            this.status = SupportStatus.IN_PROGRESS;
        }
        this.updatedAt = LocalDateTime.now();
    }

    // ✅ NOUVELLES MÉTHODES POUR LA GESTION DES STATUTS

    /**
     * Changer le statut du ticket avec validation
     */
    public void changeStatus(SupportStatus newStatus, UserEntity admin) {
        if (admin == null) {
            throw new RuntimeException("Admin is required to change ticket status");
        }

        // Validation des transitions de statut
        if (!isValidStatusTransition(this.status, newStatus)) {
            throw new RuntimeException("Invalid status transition from " + this.status + " to " + newStatus);
        }

        this.status = newStatus;
        this.assignedAdmin = admin;
        this.updatedAt = LocalDateTime.now();

        // Mettre à jour les dates spécifiques selon le statut
        switch (newStatus) {
            case RESOLVED:
                this.resolvedAt = LocalDateTime.now();
                break;
            case CLOSED:
                this.closedAt = LocalDateTime.now();
                break;
            case IN_PROGRESS:
                // Pas de date spécifique pour IN_PROGRESS
                break;
            case OPEN:
                // Réouverture d'un ticket
                this.resolvedAt = null;
                this.closedAt = null;
                break;
        }
    }

    /**
     * Changer la priorité du ticket
     */
    public void changePriority(TicketPriority newPriority, UserEntity admin) {
        if (admin == null) {
            throw new RuntimeException("Admin is required to change ticket priority");
        }

        this.priority = newPriority;
        this.assignedAdmin = admin;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Mettre à jour le ticket avec statut et priorité
     */
    public void updateTicket(SupportStatus newStatus, TicketPriority newPriority, UserEntity admin) {
        if (admin == null) {
            throw new RuntimeException("Admin is required to update ticket");
        }

        // Changer le statut si différent
        if (newStatus != null && !newStatus.equals(this.status)) {
            changeStatus(newStatus, admin);
        }

        // Changer la priorité si différente
        if (newPriority != null && !newPriority.equals(this.priority)) {
            changePriority(newPriority, admin);
        }
    }

    /**
     * Validation des transitions de statut autorisées
     */
    private boolean isValidStatusTransition(SupportStatus currentStatus, SupportStatus newStatus) {
        return switch (currentStatus) {
            case OPEN -> newStatus == SupportStatus.IN_PROGRESS || newStatus == SupportStatus.CLOSED;
            case IN_PROGRESS -> newStatus == SupportStatus.RESOLVED || newStatus == SupportStatus.CLOSED || newStatus == SupportStatus.OPEN;
            case RESOLVED -> newStatus == SupportStatus.CLOSED || newStatus == SupportStatus.OPEN;
            case CLOSED -> newStatus == SupportStatus.OPEN; // Réouverture possible
        };
    }

    /**
     * Vérifier si le ticket peut être modifié
     */
    public boolean canBeModified() {
        return status != SupportStatus.CLOSED;
    }

    /**
     * Vérifier si le ticket est urgent
     */
    public boolean isUrgent() {
        return priority == TicketPriority.URGENT || priority == TicketPriority.HIGH;
    }

    /**
     * Obtenir le temps de résolution en heures
     */
    public double getResolutionTimeHours() {
        if (resolvedAt == null) {
            return 0.0;
        }
        return java.time.Duration.between(createdAt, resolvedAt).toHours();
    }

    public boolean isActive() {
        return status == SupportStatus.OPEN || status == SupportStatus.IN_PROGRESS;
    }

    public boolean isClosed() {
        return status == SupportStatus.CLOSED || status == SupportStatus.RESOLVED;
    }
}

