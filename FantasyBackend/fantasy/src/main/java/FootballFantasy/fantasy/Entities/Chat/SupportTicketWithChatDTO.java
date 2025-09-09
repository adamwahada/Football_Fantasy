package FootballFantasy.fantasy.Entities.Chat;

import FootballFantasy.fantasy.Dto.ChatDto.ChatRoomDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicketWithChatDTO {
    private SupportTicketDTO ticket;
    private ChatRoomDTO chatRoom;
}