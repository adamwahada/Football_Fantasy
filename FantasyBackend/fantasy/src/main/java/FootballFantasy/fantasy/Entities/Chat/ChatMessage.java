package FootballFantasy.fantasy.Entities.Chat;

import FootballFantasy.fantasy.Entities.UserEntity.UserEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chat_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    private MessageType type; // TEXT, IMAGE, FILE, AUDIO, VIDEO

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private UserEntity sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_id")
    private ChatMessage replyTo; // Pour les réponses

    @CreationTimestamp
    private LocalDateTime timestamp;

    private LocalDateTime editedAt;
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;


    @Column(name = "is_edited", nullable = false)
    private Boolean isEdited = false;


    // Métadonnées pour les fichiers
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String mimeType;

    @JsonIgnore
    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MessageStatus> messageStatuses = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "replyTo", cascade = CascadeType.ALL)
    private List<ChatMessage> replies = new ArrayList<>();
}
