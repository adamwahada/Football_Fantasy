package FootballFantasy.fantasy.Dto.ChatDto;

import FootballFantasy.fantasy.Entities.Chat.ChatRoomType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomDTO {
    private Long id;
    private String roomId;
    private ChatRoomType type;
    private String name;
    private String description;
    private String avatar;
    private LocalDateTime lastActivity;
    private List<ChatParticipantDTO> participants;
    private Long unreadCount;
    private ChatMessageDTO lastMessage;
}