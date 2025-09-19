package FootballFantasy.fantasy.Exceptions.PaiementExceptions;

import FootballFantasy.fantasy.Entities.PaiementEntities.TransactionStatus;

public class InvalidWithdrawStatusException extends RuntimeException {
    public InvalidWithdrawStatusException(TransactionStatus status) {
        super("Cannot cancel withdraw request. Current status: " + status);
    }
}