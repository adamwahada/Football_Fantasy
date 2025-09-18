package FootballFantasy.fantasy.Repositories.PaiementRepositories;

import FootballFantasy.fantasy.Entities.PaiementEntities.PrefixedAmount;
import FootballFantasy.fantasy.Entities.PaiementEntities.WithdrawRequestEntity;
import FootballFantasy.fantasy.Entities.UserEntities.UserEntity;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WithdrawRequestRepository extends JpaRepository<WithdrawRequestEntity, Long> {
    List<WithdrawRequestEntity> findByRequester(UserEntity requester);
    List<WithdrawRequestEntity> findByStatus(TransactionStatus status);
    List<WithdrawRequestEntity> findByPrefixedAmountAndStatus(PrefixedAmount amount, TransactionStatus status);
}