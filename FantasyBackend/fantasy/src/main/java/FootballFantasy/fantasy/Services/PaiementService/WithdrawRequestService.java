package FootballFantasy.fantasy.Services.PaiementService;

import FootballFantasy.fantasy.Entities.PaiementEntities.*;
import FootballFantasy.fantasy.Entities.UserEntities.UserEntity;
import FootballFantasy.fantasy.Exceptions.PaiementExceptions.InsufficientBalanceException;
import FootballFantasy.fantasy.Exceptions.UsersExceptions.UserBannedException;
import FootballFantasy.fantasy.Exceptions.UsersExceptions.UserNotFoundException;
import FootballFantasy.fantasy.Repositories.PaiementRepositories.DepositTransactionRepository;
import FootballFantasy.fantasy.Repositories.PaiementRepositories.WithdrawRequestRepository;
import FootballFantasy.fantasy.Repositories.UserRepositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static FootballFantasy.fantasy.Entities.PaiementEntities.TransactionStatus.PENDING;

@Service
@RequiredArgsConstructor
public class WithdrawRequestService {

    private final WithdrawRequestRepository withdrawRepo;
    private final UserRepository userRepository;

    @Transactional
    public WithdrawRequestEntity submitWithdrawRequest(
            String keycloakId,
            BigDecimal amount,
            PrefixedAmount prefixedAmount,
            PaymentPlatform platform,
            String withdrawNumber) {

        UserEntity user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new UserNotFoundException(keycloakId));

        if (user.isBanned()) {
            throw new UserBannedException("Your account is temporarily banned");
        }

        if (user.getWithdrawableBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    keycloakId,
                    amount.toPlainString(),
                    user.getWithdrawableBalance().toPlainString()
            );
        }

        WithdrawRequestEntity withdraw = new WithdrawRequestEntity();
        withdraw.setAmount(amount);
        withdraw.setPrefixedAmount(prefixedAmount);
        withdraw.setPlatform(platform);
        withdraw.setWithdrawNumber(withdrawNumber);
        withdraw.setStatus(PENDING);
        withdraw.setReserved(false);
        withdraw.setCreatedAt(LocalDateTime.now());

        return withdrawRepo.save(withdraw);
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
        List<WithdrawRequestEntity> expired = withdrawRepo
                .findAllByReservedTrueAndReservedAtBeforeAndStatus(expiryThreshold, TransactionStatus.PENDING);

        for (WithdrawRequestEntity w : expired) {
            w.setReserved(false);
            w.setReservedAt(null);
            w.setReservedByKeycloakId(null);
            withdrawRepo.save(w);
        }
    }

}
