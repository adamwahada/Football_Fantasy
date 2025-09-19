package FootballFantasy.fantasy.Exceptions.PaiementExceptions;

import lombok.Getter;

@Getter
public class WithdrawNotReservedByUserException extends RuntimeException {

    public WithdrawNotReservedByUserException() {
        super("You can only confirm a withdraw that you have reserved.");
    }

    public WithdrawNotReservedByUserException(String message) {
        super(message);
    }
}
