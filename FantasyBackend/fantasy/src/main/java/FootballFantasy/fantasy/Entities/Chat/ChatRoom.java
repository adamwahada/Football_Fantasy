package FootballFantasy.fantasy.Entities.Chat;

import FootballFantasy.fantasy.Entities.UserEntity.UserEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chat_rooms")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String roomId; // UUID pour identifier la room

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20, nullable = false) // Augmenté à 20 caractères

    private ChatRoomType type; // PRIVATE, GROUP

    private String name; // Pour les groupes
    private String description; // Pour les groupes
    private String avatar; // URL de l'avatar du groupe

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime lastActivity;
    @Column(name = "user_id_1")
    private Long userId1;

    @Column(name = "user_id_2")
    private Long userId2;


    @JsonIgnore
    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMessage> messages = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatParticipant> participants = new ArrayList<>();

    // Méthodes utilitaires
    public void updateLastActivity() {
        this.lastActivity = LocalDateTime.now();
    }



    //support ticket
    @Column(name = "is_support_chat")
    private boolean isSupportChat = false;

    @Column(name = "support_ticket_id")
    private String supportTicketId; // Format: "TICKET-001"

    @Enumerated(EnumType.STRING)
    @Column(name = "support_status", length = 15)
    private SupportStatus supportStatus; // OPEN, IN_PROGRESS, RESOLVED, CLOSED

    @Enumerated(EnumType.STRING)
    @Column(name = "support_type")
    private SupportType supportType; // PAYMENT, TECHNICAL, ACCOUNT, GENERAL
}