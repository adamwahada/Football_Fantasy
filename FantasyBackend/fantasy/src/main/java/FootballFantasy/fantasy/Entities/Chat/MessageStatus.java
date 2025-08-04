package FootballFantasy.fantasy.Entities.Chat;

import FootballFantasy.fantasy.Entities.UserEntity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "message_statuses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private ChatMessage message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    private MessageStatusType status; // SENT, DELIVERED, READ

    @CreationTimestamp
    private LocalDateTime timestamp;
}