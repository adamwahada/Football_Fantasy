package FootballFantasy.fantasy.Controller.PaiementControllers;

import FootballFantasy.fantasy.Entities.PaiementEntities.DepositTransactionEntity;
import FootballFantasy.fantasy.Entities.PaiementEntities.PaymentPlatform;
import FootballFantasy.fantasy.Entities.PaiementEntities.PrefixedAmount;
import FootballFantasy.fantasy.Services.PaiementService.DepositTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/deposit-transactions")
@RequiredArgsConstructor
public class DepositTransactionController {

    private final DepositTransactionService depositService;

    @PostMapping("/depositdemand")
    @Operation(summary = "Create a deposit request")
    public DepositTransactionEntity createDeposit(
            @RequestParam PrefixedAmount prefixedAmount,
            @RequestParam PaymentPlatform platform,
            @RequestParam String screenshotUrl
    ) {
        // Get the current logged-in user's Keycloak ID
        String keycloakId = depositService.getCurrentUserKeycloakId();

        // Call service without passing 'amount'; service will compute it from 'prefixedAmount'
        return depositService.createDeposit(keycloakId, prefixedAmount, platform, screenshotUrl);
    }

    @PostMapping("/{depositId}/approve")
    @Operation(summary = "Approve a deposit request (Admin)")
    public DepositTransactionEntity approveDeposit(@PathVariable Long depositId) {
        String adminKeycloakId = depositService.getCurrentUserKeycloakId();
        return depositService.approveDeposit(depositId, adminKeycloakId);
    }

    @PostMapping("/{depositId}/reject")
    @Operation(summary = "Reject a deposit request (Admin)")
    public DepositTransactionEntity rejectDeposit(@PathVariable Long depositId,
                                                  @RequestParam String reason) {
        String adminKeycloakId = depositService.getCurrentUserKeycloakId();
        return depositService.rejectDeposit(depositId, adminKeycloakId, reason);
    }
}

