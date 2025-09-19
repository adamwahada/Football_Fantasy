package FootballFantasy.fantasy.Repositories.PaiementRepositories;

import FootballFantasy.fantasy.Entities.PaiementEntities.PaymentPlatform;
import FootballFantasy.fantasy.Entities.PaiementEntities.PrefixedAmount;
import FootballFantasy.fantasy.Entities.PaiementEntities.TransactionStatus;
import FootballFantasy.fantasy.Entities.PaiementEntities.WithdrawRequestEntity;
import FootballFantasy.fantasy.Entities.UserEntities.UserEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WithdrawRequestRepository extends JpaRepository<WithdrawRequestEntity, Long> {
    List<WithdrawRequestEntity> findByRequester(UserEntity requester);
    List<WithdrawRequestEntity> findByStatus(TransactionStatus status);
    List<WithdrawRequestEntity> findByPrefixedAmountAndStatus(PrefixedAmount amount, TransactionStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<WithdrawRequestEntity> findFirstByStatusAndReservedFalseAndPrefixedAmountAndPlatformOrderByCreatedAtAsc(
            TransactionStatus status,
            PrefixedAmount prefixedAmount,
            PaymentPlatform platform
    );

    List<WithdrawRequestEntity> findAllByReservedTrueAndReservedAtBeforeAndStatus(
            LocalDateTime before, TransactionStatus status
    );

    long countByRequesterAndStatusIn(UserEntity requester, List<TransactionStatus> statuses);

    boolean existsByReservedTrueAndReservedByKeycloakIdAndStatus(String keycloakId, TransactionStatus status);


}