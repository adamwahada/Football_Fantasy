package FootballFantasy.fantasy.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class UserSessionStats {
    private int totalSessions;
    private int wonSessions;
    private BigDecimal totalWinnings;
    private BigDecimal totalSpent;
    private double averageAccuracy;
    private BigDecimal netProfit;
    private double winRate;
}
