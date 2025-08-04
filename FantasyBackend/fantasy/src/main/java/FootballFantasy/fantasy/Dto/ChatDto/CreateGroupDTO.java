package FootballFantasy.fantasy.Dto.ChatDto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGroupDTO {
    @NotBlank
    private String name;

    private String description;
    private String avatar;

    @NotEmpty
    private List<Long> participantIds;
}