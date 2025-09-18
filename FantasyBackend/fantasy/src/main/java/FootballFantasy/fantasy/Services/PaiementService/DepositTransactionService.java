package FootballFantasy.fantasy.Services.PaiementService;

import FootballFantasy.fantasy.Entities.PaiementEntities.DepositTransactionEntity;
import FootballFantasy.fantasy.Entities.PaiementEntities.PaymentPlatform;
import FootballFantasy.fantasy.Entities.PaiementEntities.PrefixedAmount;
import FootballFantasy.fantasy.Entities.PaiementEntities.TransactionStatus;
import FootballFantasy.fantasy.Entities.UserEntities.UserEntity;
import FootballFantasy.fantasy.Exception.DepositNotFoundException;
import FootballFantasy.fantasy.Exception.UserBannedException;
import FootballFantasy.fantasy.Repositories.PaiementRepositories.DepositTransactionRepository;
import FootballFantasy.fantasy.Repositories.UserRepositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DepositTransactionService {

    private final DepositTransactionRepository depositRepo;
    private final UserRepository userRepository;

    /**
     * User creates a deposit request
     */
    @Transactional
    public DepositTransactionEntity createDeposit(String keycloakId,
                                                  PrefixedAmount prefixedAmount,
                                                  PaymentPlatform platform,
                                                  String screenshotUrl) {

        UserEntity user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException("User not found for Keycloak ID: " + keycloakId));

        if (user.isBanned()) {
            throw new UserBannedException("Your account is temporarily banned");
        }

        DepositTransactionEntity deposit = new DepositTransactionEntity();
        deposit.setDepositor(user);
        deposit.setPrefixedAmount(prefixedAmount);
        deposit.setAmount(BigDecimal.valueOf(prefixedAmount.getValue())); // <-- compute from enum
        deposit.setPlatform(platform);
        deposit.setScreenshotUrl(screenshotUrl);
        deposit.setStatus(TransactionStatus.PENDING);
        deposit.setCreatedAt(LocalDateTime.now());

        return depositRepo.save(deposit);
    }

    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public DepositTransactionEntity approveDeposit(Long depositId, String adminKeycloakId) {
        UserEntity admin = userRepository.findByKeycloakId(adminKeycloakId)
                .orElseThrow(() -> new RuntimeException("Admin user not found"));

        DepositTransactionEntity deposit = depositRepo.findById(depositId)
                .orElseThrow(() -> new DepositNotFoundException(depositId));

        if (deposit.getStatus() != TransactionStatus.PENDING) {
            throw new IllegalStateException("Deposit already processed");
        }

        deposit.setStatus(TransactionStatus.APPROVED);
        deposit.setApprovedBy(admin);
        deposit.setUpdatedAt(LocalDateTime.now());

        UserEntity user = deposit.getDepositor();
        user.setBalance(user.getBalance().add(deposit.getAmount()));
        user.setWithdrawableBalance(user.getWithdrawableBalance().add(deposit.getAmount()));

        return depositRepo.save(deposit);
    }

    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public DepositTransactionEntity rejectDeposit(Long depositId, String adminKeycloakId, String reason) {
        UserEntity admin = userRepository.findByKeycloakId(adminKeycloakId)
                .orElseThrow(() -> new RuntimeException("Admin user not found"));

        DepositTransactionEntity deposit = depositRepo.findById(depositId)
                .orElseThrow(() -> new DepositNotFoundException(depositId));

        if (deposit.getStatus() != TransactionStatus.PENDING) {
            throw new IllegalStateException("Deposit already processed");
        }

        deposit.setStatus(TransactionStatus.REJECTED);
        deposit.setApprovedBy(admin);
        deposit.setUpdatedAt(LocalDateTime.now());

        return depositRepo.save(deposit);
    }

    // ===== Helper to get current Keycloak ID =====
    public String getCurrentUserKeycloakId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null) {
            return authentication.getName(); // Keycloak ID from JWT
        }
        throw new RuntimeException("User not authenticated");
    }
}
