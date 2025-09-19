package FootballFantasy.fantasy.Schedulers.PaiementSchedulers;

import FootballFantasy.fantasy.Services.PaiementService.WithdrawRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WithdrawCleanupScheduler {

    private final WithdrawRequestService withdrawRequestService;

    // Runs every minute
    @Scheduled(fixedRate = 60000)
    public void cleanupExpiredReservations() {

        withdrawRequestService.releaseExpiredReservations();
    }
}