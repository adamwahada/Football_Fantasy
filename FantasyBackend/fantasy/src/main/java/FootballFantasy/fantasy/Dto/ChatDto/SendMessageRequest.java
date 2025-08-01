package FootballFantasy.fantasy.Dto.ChatDto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {
    @NotBlank(message = "Le contenu du message ne peut pas être vide")
    private String content;

    @NotNull(message = "L'ID du destinataire est requis")
    private Long receiverId;
}
