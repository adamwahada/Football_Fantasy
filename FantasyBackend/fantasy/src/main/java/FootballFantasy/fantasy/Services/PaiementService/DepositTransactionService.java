package FootballFantasy.fantasy.Services.PaiementService;

import FootballFantasy.fantasy.Dto.WithdrawReservationResponseDTO;
import FootballFantasy.fantasy.Entities.PaiementEntities.*;
import FootballFantasy.fantasy.Entities.UserEntities.UserEntity;
import FootballFantasy.fantasy.Exceptions.PaiementExceptions.*;
import FootballFantasy.fantasy.Exceptions.UsersExceptions.UserBannedException;
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
// Phase 1 – Reserve Withdraw Number
// ===========================
    @Transactional
    public WithdrawReservationResponseDTO reserveWithdrawNumber(String keycloakId,
                                                                PrefixedAmount prefixedAmount,
                                                                PaymentPlatform platform) {
        UserEntity user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new UserNotFoundException(keycloakId));

        if (user.isBanned()) throw new UserBannedException("Your account is temporarily banned");

        // ✅ Check if user already has a reserved withdraw not yet processed
        boolean hasActiveReservation = withdrawRepo.existsByReservedTrueAndReservedByKeycloakIdAndStatus(
                user.getKeycloakId(),
                TransactionStatus.RESERVED
        );
        if (hasActiveReservation) {
            throw new WithdrawLimitExceededException("You already have an active reserved deposit. Complete it first.");
        }

        // ✅ Count pending or in-review deposits to limit spam
        long pendingWithdraws = withdrawRepo.countByRequesterAndStatusIn(
                user, List.of(TransactionStatus.PENDING, TransactionStatus.IN_REVIEW)
        );
        if (pendingWithdraws >= 2) { // limit to 2 deposits waiting for admin validation
            throw new WithdrawLimitExceededException("You cannot have more than 2 deposits awaiting for admin validation.");
        }

        WithdrawRequestEntity withdrawRequest = withdrawRepo
                .findFirstByStatusAndReservedFalseAndPrefixedAmountAndPlatformOrderByCreatedAtAsc(
                        TransactionStatus.PENDING,
                        prefixedAmount,
                        platform
                )
                .orElseThrow(WithdrawNotAvailableException::new);

        withdrawRequest.setReserved(true);
        withdrawRequest.setReservedAt(LocalDateTime.now());
        withdrawRequest.setReservedByKeycloakId(keycloakId);
        withdrawRequest.setStatus(TransactionStatus.RESERVED);

        withdrawRepo.save(withdrawRequest);

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
// Phase 2 – Confirm Deposit (uses reservation values only)
// ===========================
    @Transactional
    public DepositTransactionEntity confirmDeposit(String keycloakId,
                                                   String screenshotUrl,
                                                   Long withdrawId) {

        UserEntity user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isBanned()) throw new UserBannedException("Your account is temporarily banned");

        WithdrawRequestEntity withdrawRequest = withdrawRepo.findById(withdrawId)
                .orElseThrow(WithdrawNotFoundException::new);

        // ensure it was reserved and by the same user
        if (!withdrawRequest.isReserved() ||
                !keycloakId.equals(withdrawRequest.getReservedByKeycloakId()) ||
                withdrawRequest.getStatus() != TransactionStatus.RESERVED) {
            throw new WithdrawNotReservedByUserException();
        }

        // check expiry
        LocalDateTime expiryThreshold = withdrawRequest.getReservedAt().plusMinutes(15);
        if (LocalDateTime.now().isAfter(expiryThreshold)) {
            withdrawRequest.setReserved(false);
            withdrawRequest.setReservedAt(null);
            withdrawRequest.setReservedByKeycloakId(null);
            withdrawRequest.setStatus(TransactionStatus.PENDING);
            withdrawRepo.save(withdrawRequest);
            throw new WithdrawReservationExpiredException();
        }

        // Move from RESERVED → IN_REVIEW
        withdrawRequest.setStatus(TransactionStatus.IN_REVIEW);
        withdrawRepo.save(withdrawRequest);

        // Use values from reservation only
        DepositTransactionEntity deposit = new DepositTransactionEntity();
        deposit.setDepositor(user);
        deposit.setPrefixedAmount(withdrawRequest.getPrefixedAmount());
        deposit.setAmount(withdrawRequest.getAmount());
        deposit.setPlatform(withdrawRequest.getPlatform());
        deposit.setScreenshotUrl(screenshotUrl);
        deposit.setStatus(TransactionStatus.IN_REVIEW);
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

        UserEntity depositor = deposit.getDepositor();
        BigDecimal amount = deposit.getAmount();

        // ✅ Add only to balance (usable in system)
        depositor.setBalance(depositor.getBalance().add(amount));

        // ✅ Remove the amount from pendingDeposits
        depositor.setPendingDeposits(depositor.getPendingDeposits().subtract(amount));
        if (depositor.getPendingDeposits().compareTo(BigDecimal.ZERO) < 0) {
            depositor.setPendingDeposits(BigDecimal.ZERO);
        }

        userRepository.save(depositor);

        // Handle matched withdraw request if exists
        WithdrawRequestEntity withdraw = deposit.getMatchedWithdraw();
        if (withdraw != null && withdraw.getStatus() == TransactionStatus.IN_REVIEW) {
            withdraw.setStatus(TransactionStatus.APPROVED);
            withdraw.setUpdatedAt(LocalDateTime.now());

            UserEntity withdrawer = withdraw.getRequester();
            // Adjust pendingWithdrawals
            withdrawer.setPendingWithdrawals(withdrawer.getPendingWithdrawals().subtract(withdraw.getAmount()));
            if (withdrawer.getPendingWithdrawals().compareTo(BigDecimal.ZERO) < 0) {
                withdrawer.setPendingWithdrawals(BigDecimal.ZERO);
            }

            userRepository.save(withdrawer);
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

        UserEntity depositor = deposit.getDepositor();
        BigDecimal amount = deposit.getAmount();

        // ✅ Remove from pendingDeposits (refund not to balance)
        depositor.setPendingDeposits(depositor.getPendingDeposits().subtract(amount));
        if (depositor.getPendingDeposits().compareTo(BigDecimal.ZERO) < 0) {
            depositor.setPendingDeposits(BigDecimal.ZERO);
        }
        userRepository.save(depositor);

        WithdrawRequestEntity withdraw = deposit.getMatchedWithdraw();
        if (withdraw != null && withdraw.getStatus() == TransactionStatus.IN_REVIEW) {
            // Release reservation
            withdraw.setReserved(false);
            withdraw.setReservedAt(null);
            withdraw.setReservedByKeycloakId(null);
            withdraw.setStatus(TransactionStatus.PENDING);
            withdraw.setUpdatedAt(LocalDateTime.now());

            UserEntity withdrawer = withdraw.getRequester();
            // Adjust pendingWithdrawals
            withdrawer.setPendingWithdrawals(withdrawer.getPendingWithdrawals().subtract(withdraw.getAmount()));
            if (withdrawer.getPendingWithdrawals().compareTo(BigDecimal.ZERO) < 0) {
                withdrawer.setPendingWithdrawals(BigDecimal.ZERO);
            }

            // Refund withdraw to balance & withdrawable
            withdrawer.setBalance(withdrawer.getBalance().add(withdraw.getAmount()));
            withdrawer.setWithdrawableBalance(withdrawer.getWithdrawableBalance().add(withdraw.getAmount()));

            userRepository.save(withdrawer);
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
        return depositRepo.findByStatus(TransactionStatus.IN_REVIEW);
    }



    public String getCurrentUserKeycloakId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null) {
            return authentication.getName();
        }
        throw new RuntimeException("User not authenticated");
    }

    @Transactional
    public void notifyPendingDeposits() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);
        List<DepositTransactionEntity> pending = depositRepo.findByStatusAndCreatedAtBefore(TransactionStatus.IN_REVIEW, threshold);

        for (DepositTransactionEntity deposit : pending) {
            // send email/log reminder for admin to process
            System.out.println("Deposit ID " + deposit.getId() + " pending review for over 24h.");
        }
    }

}
