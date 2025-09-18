package FootballFantasy.fantasy.Controller.PaiementControllers;

import FootballFantasy.fantasy.Entities.PaiementEntities.*;
import FootballFantasy.fantasy.Repositories.UserRepositories.UserRepository;
import FootballFantasy.fantasy.Services.PaiementService.DepositTransactionService;
import FootballFantasy.fantasy.Services.PaiementService.WithdrawRequestService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/withdraw-requests")
@RequiredArgsConstructor
public class WithdrawRequestController {

    private final WithdrawRequestService withdrawService;
    private final DepositTransactionService depositTransactionService;

    // ===========================
    // User: Submit a new withdraw request
    // ===========================
    @PostMapping("/submit")
    @Operation(summary = "Submit a withdraw request")
    public WithdrawRequestEntity submitWithdraw(
            @RequestParam BigDecimal amount,
            @RequestParam PrefixedAmount prefixedAmount,
            @RequestParam PaymentPlatform platform,
            @RequestParam String withdrawNumber
    ) {
        String keycloakId = depositTransactionService.getCurrentUserKeycloakId();
        return withdrawService.submitWithdrawRequest(keycloakId, amount, prefixedAmount, platform, withdrawNumber);
    }

    // ===========================
    // User: Get own withdraw requests
    // ===========================
    @GetMapping("/my-requests")
    @Operation(summary = "Get all withdraw requests for the current user")
    public List<WithdrawRequestEntity> getUserWithdrawRequests() {
        String keycloakId = depositTransactionService.getCurrentUserKeycloakId();
        return withdrawService.getUserWithdrawRequests(keycloakId);
    }

    // ===========================
    // Admin: Get all withdraw requests
    // ===========================
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Get all withdraw requests (Admin)")
    public List<WithdrawRequestEntity> getAllWithdrawRequests() {
        return withdrawService.getAllWithdrawRequests();
    }

    // ===========================
    // Admin: Release expired reservations manually
    // ===========================
    @PostMapping("/release-expired")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Release expired withdraw reservations (Admin)")
    public void releaseExpiredWithdrawReservations() {
        withdrawService.releaseExpiredReservations();
    }
}
