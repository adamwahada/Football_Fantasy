package FootballFantasy.fantasy.Dto.ChatDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkAsReadDTO {
    private String roomId;
    private Long messageId;
}