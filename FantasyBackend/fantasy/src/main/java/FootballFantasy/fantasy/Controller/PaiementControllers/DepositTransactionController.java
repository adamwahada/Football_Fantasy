package FootballFantasy.fantasy.Controller.PaiementControllers;

import FootballFantasy.fantasy.Dto.WithdrawReservationResponseDTO;
import FootballFantasy.fantasy.Entities.PaiementEntities.*;
import FootballFantasy.fantasy.Services.PaiementService.DepositTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/deposit-transactions")
@RequiredArgsConstructor
public class DepositTransactionController {

    private final DepositTransactionService depositService;

    // ===========================
    // Phase 1 – Reserve Withdraw Number
    // ===========================
    @PostMapping("/reserve")
    @Operation(summary = "Reserve a withdraw number before confirming deposit")
    public WithdrawReservationResponseDTO reserveWithdraw(
            @RequestParam PrefixedAmount prefixedAmount,
            @RequestParam PaymentPlatform platform
    ) {
        String keycloakId = depositService.getCurrentUserKeycloakId();
        return depositService.reserveWithdrawNumber(keycloakId, prefixedAmount, platform);
    }

    // ===========================
    // Phase 2 – Confirm Deposit
    // ===========================
    @PostMapping("/confirm")
    @Operation(summary = "Confirm a deposit using the reserved withdraw number")
    public DepositTransactionEntity confirmDeposit(
            @RequestParam Long withdrawId,
            @RequestParam PrefixedAmount prefixedAmount,
            @RequestParam PaymentPlatform platform,
            @RequestParam String screenshotUrl
    ) {
        String keycloakId = depositService.getCurrentUserKeycloakId();
        return depositService.confirmDeposit(keycloakId, prefixedAmount, platform, screenshotUrl, withdrawId);
    }

    // ===========================
    // Admin: Approve Deposit
    // ===========================
    @PostMapping("/{depositId}/approve")
    @Operation(summary = "Approve a deposit request (Admin)")
    public DepositTransactionEntity approveDeposit(@PathVariable Long depositId) {
        String adminKeycloakId = depositService.getCurrentUserKeycloakId();
        return depositService.approveDeposit(depositId, adminKeycloakId);
    }

    // ===========================
    // Admin: Reject Deposit
    // ===========================
    @PostMapping("/{depositId}/reject")
    @Operation(summary = "Reject a deposit request (Admin)")
    public DepositTransactionEntity rejectDeposit(@PathVariable Long depositId) {
        String adminKeycloakId = depositService.getCurrentUserKeycloakId();
        return depositService.rejectDeposit(depositId, adminKeycloakId);
    }

    // ===========================
    // User: Get own deposits
    // ===========================
    @GetMapping("/my-deposits")
    @Operation(summary = "Get all deposits for the current user, optionally filtered by status")
    public List<DepositTransactionEntity> getUserDeposits(
            @RequestParam(required = false) TransactionStatus status
    ) {
        return depositService.getUserDeposits(status);
    }

    // ===========================
    // Admin: Get pending deposits
    // ===========================
    @GetMapping("/reviewable-deposits")
    @Operation(summary = "Get all deposits waiting for review (Admin)")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<DepositTransactionEntity> getDepositsInReview() {
        return depositService.getDepositsInReview();
    }
}
