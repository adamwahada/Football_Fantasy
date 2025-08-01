package FootballFantasy.fantasy.Dto.ChatDto;

import FootballFantasy.fantasy.Entities.Chat.MessageStatus;
import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageDTO {
    private Long id;
    private String content;
    private Long senderId;
    private String senderUsername;
    private String senderFirstName;
    private String senderLastName;
    private Long receiverId;
    private String receiverUsername;
    private String receiverFirstName;
    private String receiverLastName;
    private LocalDateTime sentAt;
    private Boolean isRead;
    private MessageStatus status;
}
