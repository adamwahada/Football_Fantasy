package FootballFantasy.fantasy.Exception;

// Create custom exception for business logic errors
public class BusinessLogicException extends RuntimeException {
    private final String errorCode;

    public BusinessLogicException(String message) {
        super(message);
        this.errorCode = "BUSINESS_ERROR";
    }

    public BusinessLogicException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}