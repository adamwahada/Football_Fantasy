package FootballFantasy.fantasy.Exceptions.PaiementExceptions;

import lombok.Getter;

@Getter
public class WithdrawNotAvailableException extends RuntimeException {

    public WithdrawNotAvailableException() {
        super("No withdraw requests available");
    }
}
