package FootballFantasy.fantasy.Exceptions.PaiementExceptions;

import lombok.Getter;

@Getter
public class WithdrawLimitExceededException extends RuntimeException {

    public WithdrawLimitExceededException(String message) {
        super(message);
    }
}
