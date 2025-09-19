package FootballFantasy.fantasy.Exceptions.PaiementExceptions;

public class WithdrawReservedException extends RuntimeException {
    public WithdrawReservedException() {
        super("Cannot cancel withdraw request - it's currently reserved.");
    }
}