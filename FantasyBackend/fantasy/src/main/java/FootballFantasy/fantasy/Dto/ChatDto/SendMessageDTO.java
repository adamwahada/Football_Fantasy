package FootballFantasy.fantasy.Dto.ChatDto;

import FootballFantasy.fantasy.Entities.Chat.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageDTO {
    @NotBlank
    private String roomId;

    @NotBlank
    private String content;

    @NotNull
    private MessageType type = MessageType.TEXT;

    private Long replyToId;

    // Pour les fichiers
    private String fileName;
    private String fileUrl;
    private Long fileSize;
    private String mimeType;
}