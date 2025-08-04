package FootballFantasy.fantasy.Dto.ChatDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {
    private String type; // MESSAGE, TYPING, USER_STATUS, MESSAGE_STATUS
    private String roomId;
    private Long senderId;
    private String senderName;
    private Object payload;
}