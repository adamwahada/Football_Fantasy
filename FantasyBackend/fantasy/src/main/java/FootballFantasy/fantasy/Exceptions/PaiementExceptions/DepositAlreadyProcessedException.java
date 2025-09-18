package FootballFantasy.fantasy.Exceptions.PaiementExceptions;

import lombok.Getter;

@Getter
public class DepositAlreadyProcessedException extends RuntimeException {
    private final Long depositId;

    public DepositAlreadyProcessedException(Long depositId) {
        super(String.format("Deposit with ID %s has already been processed", depositId));
        this.depositId = depositId;
    }
}
