package FootballFantasy.fantasy.Dto.ChatDto;

import FootballFantasy.fantasy.Entities.Chat.ParticipantRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatParticipantDTO {
    private Long id;
    private Long userId;
    private String username;
    private String fullName;
    private String avatar;
    private ParticipantRole role;
    private LocalDateTime joinedAt;
    private LocalDateTime lastSeenAt;
    private Boolean isActive;
    private Boolean isMuted;
    private Boolean isOnline;
}
