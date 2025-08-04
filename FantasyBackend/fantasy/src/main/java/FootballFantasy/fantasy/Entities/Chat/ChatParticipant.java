package FootballFantasy.fantasy.Entities.Chat;

import FootballFantasy.fantasy.Entities.UserEntity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_participants")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @Enumerated(EnumType.STRING)
    private ParticipantRole role; // ADMIN, MEMBER

    @CreationTimestamp
    private LocalDateTime joinedAt;

    private LocalDateTime lastSeenAt;
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_muted", nullable = false)
    @Builder.Default
    private Boolean isMuted = false;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
