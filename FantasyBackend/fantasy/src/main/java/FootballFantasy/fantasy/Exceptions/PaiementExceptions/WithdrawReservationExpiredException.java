package FootballFantasy.fantasy.Exceptions.PaiementExceptions;

import lombok.Getter;

@Getter
public class WithdrawReservationExpiredException extends RuntimeException {

    public WithdrawReservationExpiredException() {
        super("Reservation expired. Please try again.");
    }

    public WithdrawReservationExpiredException(String message) {
        super(message);
    }
}
