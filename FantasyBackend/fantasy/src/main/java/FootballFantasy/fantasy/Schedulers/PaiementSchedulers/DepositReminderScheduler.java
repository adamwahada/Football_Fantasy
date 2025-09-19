package FootballFantasy.fantasy.Schedulers.PaiementSchedulers;

import FootballFantasy.fantasy.Services.PaiementService.DepositTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DepositReminderScheduler {

    private final DepositTransactionService depositTransactionService;

    // Runs every hour
    @Scheduled(fixedRate = 3600000)
    public void remindAdminForPendingDeposits() {

        depositTransactionService.notifyPendingDeposits();
    }
}