package FootballFantasy.fantasy.Repositories.PaiementRepositories;

import FootballFantasy.fantasy.Entities.PaiementEntities.DepositTransactionEntity;
import FootballFantasy.fantasy.Entities.PaiementEntities.WithdrawRequestEntity;
import FootballFantasy.fantasy.Entities.UserEntities.UserEntity;
import FootballFantasy.fantasy.Entities.PaiementEntities.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DepositTransactionRepository extends JpaRepository<DepositTransactionEntity, Long> {
    List<DepositTransactionEntity> findByDepositor(UserEntity depositor);
    List<DepositTransactionEntity> findByStatus(TransactionStatus status);
    List<DepositTransactionEntity> findByDepositorAndStatus(UserEntity depositor, TransactionStatus status);
    boolean existsByMatchedWithdraw(WithdrawRequestEntity withdrawRequest);

}