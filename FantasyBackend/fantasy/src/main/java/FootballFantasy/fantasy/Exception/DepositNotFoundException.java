package FootballFantasy.fantasy.Exception;

import lombok.Getter;

// Create custom exception for deposit not found
@Getter
public class DepositNotFoundException extends RuntimeException {
    private final String depositId;

    public DepositNotFoundException(Long depositId) {
        super(String.format("Deposit with ID %s not found", depositId));
        this.depositId = String.valueOf(depositId);
    }
}