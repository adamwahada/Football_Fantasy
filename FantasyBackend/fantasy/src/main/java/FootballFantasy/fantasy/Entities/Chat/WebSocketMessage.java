package FootballFantasy.fantasy.Entities.Chat;

import FootballFantasy.fantasy.Dto.ChatDto.MessageDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebSocketMessage {
    private String type; // "MESSAGE", "TYPING", "USER_ONLINE", "USER_OFFLINE"
    private MessageDTO message;
    private Long userId;
    private String username;
    private Object data;
}