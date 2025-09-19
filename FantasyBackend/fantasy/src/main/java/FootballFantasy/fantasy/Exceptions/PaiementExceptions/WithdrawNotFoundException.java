package FootballFantasy.fantasy.Exceptions.PaiementExceptions;

public class WithdrawNotFoundException extends RuntimeException {
    public WithdrawNotFoundException() {
        super("Withdraw request was not found.");
    }
}
