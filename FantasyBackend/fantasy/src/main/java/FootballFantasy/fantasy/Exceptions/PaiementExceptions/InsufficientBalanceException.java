package FootballFantasy.fantasy.Exceptions.PaiementExceptions;

import lombok.Getter;

// Create custom exception for insufficient balance
@Getter
public class InsufficientBalanceException extends RuntimeException {
    private final String userId;
    private final String requiredAmount;
    private final String currentBalance;

    public InsufficientBalanceException(String userId, String requiredAmount, String currentBalance) {
        super(String.format("Insufficient balance for user %s: required %s, current balance %s",
                userId, requiredAmount, currentBalance));
        this.userId = userId;
        this.requiredAmount = requiredAmount;
        this.currentBalance = currentBalance;
    }

}

