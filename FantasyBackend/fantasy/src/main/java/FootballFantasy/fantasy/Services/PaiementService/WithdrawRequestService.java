package FootballFantasy.fantasy.Services.PaiementService;

import FootballFantasy.fantasy.Entities.PaiementEntities.*;
import FootballFantasy.fantasy.Entities.UserEntities.UserEntity;
import FootballFantasy.fantasy.Exceptions.PaiementExceptions.*;
import FootballFantasy.fantasy.Exceptions.UsersExceptions.UserBannedException;
import FootballFantasy.fantasy.Exceptions.UsersExceptions.UserNotFoundException;
import FootballFantasy.fantasy.Repositories.PaiementRepositories.WithdrawRequestRepository;
import FootballFantasy.fantasy.Repositories.UserRepositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WithdrawRequestService {

    private final WithdrawRequestRepository withdrawRepo;
    private final UserRepository userRepository;

    @Transactional
    public WithdrawRequestEntity submitWithdrawRequest(
            String keycloakId,
            PrefixedAmount prefixedAmount,
            PaymentPlatform platform,
            String withdrawNumber
    ) {
        UserEntity user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new UserNotFoundException(keycloakId));

        if (user.isBanned()) {
            throw new UserBannedException("Your account is temporarily banned");
        }

        BigDecimal amount = BigDecimal.valueOf(prefixedAmount.getValue());

        // ✅ Check only withdrawable balance
        if (user.getWithdrawableBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    keycloakId,
                    amount.toPlainString(),
                    user.getWithdrawableBalance().toPlainString()
            );
        }

        // ✅ Limit pending withdrawals
        long pendingWithdraws = withdrawRepo.countByRequesterAndStatusIn(
                user, List.of(TransactionStatus.PENDING, TransactionStatus.IN_REVIEW)
        );
        if (pendingWithdraws >= 3) {
            throw new WithdrawLimitExceededException("You cannot have more than 3 pending withdraw requests");
        }

        // ✅ Update balances immediately
        user.setBalance(user.getBalance().subtract(amount));
        user.setWithdrawableBalance(user.getWithdrawableBalance().subtract(amount));
        user.setPendingWithdrawals(user.getPendingWithdrawals().add(amount)); // ← use pendingWithdrawals
        userRepository.save(user);

        // ✅ Create withdraw request
        WithdrawRequestEntity withdraw = new WithdrawRequestEntity();
        withdraw.setRequester(user);
        withdraw.setAmount(amount);
        withdraw.setPrefixedAmount(prefixedAmount);
        withdraw.setPlatform(platform);
        withdraw.setWithdrawNumber(withdrawNumber);
        withdraw.setStatus(TransactionStatus.PENDING);
        withdraw.setReserved(false);
        withdraw.setCreatedAt(LocalDateTime.now());

        return withdrawRepo.save(withdraw);
    }

    @Transactional
    public void cancelWithdrawRequest(String keycloakId, Long withdrawId) {
        UserEntity user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new UserNotFoundException(keycloakId));

        WithdrawRequestEntity withdraw = withdrawRepo.findById(withdrawId)
                .orElseThrow(WithdrawNotFoundException::new);

        if (!withdraw.getRequester().getKeycloakId().equals(keycloakId)) {
            throw new UnauthorizedWithdrawCancellationException();
        }

        if (withdraw.getStatus() != TransactionStatus.PENDING) {
            throw new InvalidWithdrawStatusException(withdraw.getStatus());
        }

        if (withdraw.isReserved()) {
            throw new WithdrawReservedException();
        }

        // Refund the user's balances
        BigDecimal amount = withdraw.getAmount();
        user.setBalance(user.getBalance().add(amount));
        user.setWithdrawableBalance(user.getWithdrawableBalance().add(amount));
        user.setPendingWithdrawals(user.getPendingWithdrawals().subtract(amount)); // ← use pendingWithdrawals
        if (user.getPendingWithdrawals().compareTo(BigDecimal.ZERO) < 0) {
            user.setPendingWithdrawals(BigDecimal.ZERO);
        }

        userRepository.save(user);

        // Mark withdraw as cancelled
        withdraw.setStatus(TransactionStatus.CANCELLED);
        withdrawRepo.save(withdraw);
    }


    // ===========================
    // Admin: Get all withdraw requests
    // ===========================
    @Transactional
    public List<WithdrawRequestEntity> getAllWithdrawRequests() {
        return withdrawRepo.findAll();
    }

    // ===========================
    // User: Get withdraw requests for a specific user
    // ===========================
    @Transactional
    public List<WithdrawRequestEntity> getUserWithdrawRequests(String keycloakId) {
        UserEntity user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new UserNotFoundException(keycloakId));
        return withdrawRepo.findByRequester(user);
    }

    @Transactional
    public void releaseExpiredReservations() {
        LocalDateTime expiryThreshold = LocalDateTime.now().minusMinutes(15);

        // Only release withdraws that are reserved AND still in review
        List<WithdrawRequestEntity> expired = withdrawRepo
                .findAllByReservedTrueAndReservedAtBeforeAndStatus(expiryThreshold, TransactionStatus.RESERVED);

        for (WithdrawRequestEntity w : expired) {
            w.setReserved(false);
            w.setReservedAt(null);
            w.setReservedByKeycloakId(null);
            w.setStatus(TransactionStatus.PENDING); // put back in the pool
            withdrawRepo.save(w);
        }
    }
}
