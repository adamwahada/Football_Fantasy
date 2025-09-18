package FootballFantasy.fantasy.Dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import FootballFantasy.fantasy.Entities.PaiementEntities.PaymentPlatform;
import FootballFantasy.fantasy.Entities.PaiementEntities.PrefixedAmount;

public record WithdrawReservationResponseDTO(
        Long withdrawId,       // internal ID to confirm deposit later
        String withdrawNumber, // the number user must send money to
        BigDecimal amount,     // amount to send
        PaymentPlatform platform,
        PrefixedAmount prefixedAmount,
        LocalDateTime expiresAt

) {}
