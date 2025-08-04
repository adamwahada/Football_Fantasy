package FootballFantasy.fantasy.Dto.ChatDto;

import FootballFantasy.fantasy.Entities.Chat.MessageStatusType;
import FootballFantasy.fantasy.Entities.Chat.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {
    private Long id;
    private String content;
    private MessageType type;
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    private LocalDateTime timestamp;
    private LocalDateTime editedAt;
    private Boolean isEdited;
    private Boolean isDeleted;
    private Long replyToId;
    private ChatMessageDTO replyToMessage;
    private MessageStatusType status;

    // Pour les fichiers
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String mimeType;
}