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

    public boolean isActive() {
        return status == SupportStatus.OPEN || status == SupportStatus.IN_PROGRESS;
    }

    public boolean isClosed() {
        return status == SupportStatus.CLOSED || status == SupportStatus.RESOLVED;
    }
}

