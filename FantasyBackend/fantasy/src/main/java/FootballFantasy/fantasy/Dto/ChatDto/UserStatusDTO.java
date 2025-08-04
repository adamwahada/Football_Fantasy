package FootballFantasy.fantasy.Dto.ChatDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatusDTO {
    private Long userId;
    private String username;
    private Boolean isOnline;
    private String lastSeen;
}
