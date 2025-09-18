package FootballFantasy.fantasy.Services.PaiementService;

import FootballFantasy.fantasy.Dto.WithdrawReservationResponseDTO;
import FootballFantasy.fantasy.Entities.PaiementEntities.*;
import FootballFantasy.fantasy.Entities.UserEntities.UserEntity;
import FootballFantasy.fantasy.Exceptions.UsersExceptions.UserBannedException;
import FootballFantasy.fantasy.Exceptions.PaiementExceptions.WithdrawNotAvailableException;
import FootballFantasy.fantasy.Exceptions.PaiementExceptions.DepositAlreadyProcessedException;
import FootballFantasy.fantasy.Exceptions.PaiementExceptions.DepositNotFoundException;
import FootballFantasy.fantasy.Exceptions.UsersExceptions.UserNotFoundException;
import FootballFantasy.fantasy.Repositories.PaiementRepositories.DepositTransactionRepository;
import FootballFantasy.fantasy.Repositories.PaiementRepositories.WithdrawRequestRepository;
import FootballFantasy.fantasy.Repositories.UserRepositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DepositTransactionService {

    private final DepositTransactionRepository depositRepo;
    private final UserRepository userRepository;
    private final WithdrawRequestRepository withdrawRepo;

    // ===========================
    // Phase 1 – Pre-Deposit: Reserve Withdraw Number
    // ===========================
    @Transactional
    public WithdrawReservationResponseDTO reserveWithdrawNumber(String keycloakId,
                                                                PrefixedAmount prefixedAmount,
                                                                PaymentPlatform platform) {
        UserEntity user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new UserNotFoundException(keycloakId));

        if (user.isBanned()) throw new UserBannedException("Your account is temporarily banned");

        WithdrawRequestEntity withdrawRequest = withdrawRepo
                .findFirstByStatusAndReservedFalseOrderByCreatedAtAscForUpdate(TransactionStatus.PENDING)
                .orElseThrow(WithdrawNotAvailableException::new);

        withdrawRequest.setReserved(true);
        withdrawRequest.setReservedAt(LocalDateTime.now());
        withdrawRequest.setReservedByKeycloakId(keycloakId);
        withdrawRepo.save(withdrawRequest);

        // Calculate expiration
        LocalDateTime expiresAt = withdrawRequest.getReservedAt().plusMinutes(15);

        return new WithdrawReservationResponseDTO(
                withdrawRequest.getId(),
                withdrawRequest.getWithdrawNumber(),
                withdrawRequest.getAmount(),
                withdrawRequest.getPlatform(),
                withdrawRequest.getPrefixedAmount(),
                expiresAt
        );
    }


    // ===========================
    // Phase 2 – Confirm Deposit: Create the actual DepositTransactionEntity
    // ===========================
    @Transactional
    public DepositTransactionEntity confirmDeposit(String keycloakId,
                                                   PrefixedAmount prefixedAmount,
                                                   PaymentPlatform platform,
                                                   String screenshotUrl,
                                                   Long withdrawId) {

        UserEntity user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new UserNotFoundException(keycloakId));

        if (user.isBanned()) {
            throw new UserBannedException("Your account is temporarily banned");
        }

        // 1️⃣ Get the reserved withdraw request
        WithdrawRequestEntity withdrawRequest = withdrawRepo.findById(withdrawId)
                .orElseThrow(() -> new RuntimeException("Withdraw request not found"));

        // ❌ Reject if not reserved
        if (!withdrawRequest.isReserved()) {
            throw new RuntimeException("Withdraw request is no longer reserved");
        }

        // ⏰ Reject if 15 minutes passed
        LocalDateTime expiryThreshold = withdrawRequest.getReservedAt().plusMinutes(15);
        if (LocalDateTime.now().isAfter(expiryThreshold)) {
            withdrawRequest.setReserved(false);
            withdrawRequest.setReservedAt(null);
            withdrawRepo.save(withdrawRequest);
            throw new RuntimeException("Reservation expired. Please try again.");
        }

        // 2️⃣ Lock the withdraw (status → IN_REVIEW)
        withdrawRequest.setStatus(TransactionStatus.IN_REVIEW);
        withdrawRepo.save(withdrawRequest);

        // 3️⃣ Create deposit entity
        DepositTransactionEntity deposit = new DepositTransactionEntity();
        deposit.setDepositor(user);
        deposit.setPrefixedAmount(prefixedAmount);
        deposit.setAmount(BigDecimal.valueOf(prefixedAmount.getValue()));
        deposit.setPlatform(platform);
        deposit.setScreenshotUrl(screenshotUrl);
        deposit.setStatus(TransactionStatus.PENDING);
        deposit.setCreatedAt(LocalDateTime.now());
        deposit.setMatchedWithdraw(withdrawRequest);

        return depositRepo.save(deposit);
    }

    // ===========================
    // Approve Deposit
    // ===========================
    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public DepositTransactionEntity approveDeposit(Long depositId, String adminKeycloakId) {
        UserEntity admin = userRepository.findByKeycloakId(adminKeycloakId)
                .orElseThrow(() -> new RuntimeException("Admin user not found"));

        DepositTransactionEntity deposit = depositRepo.findById(depositId)
                .orElseThrow(() -> new DepositNotFoundException(depositId));

        if (deposit.getStatus() != TransactionStatus.IN_REVIEW) {
            throw new DepositAlreadyProcessedException(depositId);
        }

        deposit.setStatus(TransactionStatus.APPROVED);
        deposit.setApprovedBy(admin);
        deposit.setUpdatedAt(LocalDateTime.now());

        UserEntity user = deposit.getDepositor();
        user.setBalance(user.getBalance().add(deposit.getAmount()));
        user.setWithdrawableBalance(user.getWithdrawableBalance().add(deposit.getAmount()));

        WithdrawRequestEntity withdraw = deposit.getMatchedWithdraw();
        if (withdraw != null && withdraw.getStatus() == TransactionStatus.IN_REVIEW) {
            withdraw.setStatus(TransactionStatus.APPROVED);
            withdraw.setUpdatedAt(LocalDateTime.now());
            // keep reserved true if you want it for traceability
            withdrawRepo.save(withdraw);
        }

        return depositRepo.save(deposit);
    }


    // ===========================
    // Reject Deposit
    // ===========================
    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public DepositTransactionEntity rejectDeposit(Long depositId, String adminKeycloakId) {
        UserEntity admin = userRepository.findByKeycloakId(adminKeycloakId)
                .orElseThrow(() -> new RuntimeException("Admin user not found"));

        DepositTransactionEntity deposit = depositRepo.findById(depositId)
                .orElseThrow(() -> new DepositNotFoundException(depositId));

        if (deposit.getStatus() != TransactionStatus.IN_REVIEW) {
            throw new DepositAlreadyProcessedException(depositId);
        }

        deposit.setStatus(TransactionStatus.REJECTED);
        deposit.setApprovedBy(admin);
        deposit.setUpdatedAt(LocalDateTime.now());

        WithdrawRequestEntity withdraw = deposit.getMatchedWithdraw();
        if (withdraw != null && withdraw.getStatus() == TransactionStatus.IN_REVIEW) {
            // release so it can be used again
            withdraw.setReserved(false);
            withdraw.setReservedAt(null);
            withdraw.setReservedByKeycloakId(null);
            withdraw.setStatus(TransactionStatus.PENDING);
            withdrawRepo.save(withdraw);
        }

        return depositRepo.save(deposit);
    }


    // ===========================
    // Utility Methods
    // ===========================
    public List<DepositTransactionEntity> getUserDeposits(TransactionStatus status) {
        String keycloakId = getCurrentUserKeycloakId();
        UserEntity user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (status != null) {
            return depositRepo.findByDepositorAndStatus(user, status);
        } else {
            return depositRepo.findByDepositor(user);
        }
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<DepositTransactionEntity> getDepositsInReview() {
        return depositRepo.findByStatus(TransactionStatus.PENDING)
                .stream()
                .filter(d -> d.getMatchedWithdraw() != null
                        && d.getMatchedWithdraw().getStatus() == TransactionStatus.IN_REVIEW)
                .toList();
    }


    public String getCurrentUserKeycloakId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null) {
            return authentication.getName();
        }
        throw new RuntimeException("User not authenticated");
    }
}
