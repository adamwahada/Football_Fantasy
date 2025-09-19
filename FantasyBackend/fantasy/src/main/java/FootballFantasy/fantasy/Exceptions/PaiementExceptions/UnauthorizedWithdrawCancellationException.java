package FootballFantasy.fantasy.Exceptions.PaiementExceptions;

public class UnauthorizedWithdrawCancellationException extends RuntimeException {
    public UnauthorizedWithdrawCancellationException() {
        super("You can only cancel your own withdraw requests.");
    }
}