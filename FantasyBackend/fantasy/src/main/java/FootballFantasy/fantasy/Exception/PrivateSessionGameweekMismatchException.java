package FootballFantasy.fantasy.Exception;

public class PrivateSessionGameweekMismatchException extends RuntimeException {
    private final String accessKey;
    private final Long requestedGameweekId;
    private final Long actualGameweekId;
    private final String errorCode;

    public PrivateSessionGameweekMismatchException(String accessKey, Long requestedGameweekId, Long actualGameweekId) {
        super(String.format("Private session with access key '%s' exists for gameweek %d, but you're trying to join gameweek %d",
                accessKey, actualGameweekId, requestedGameweekId));
        this.accessKey = accessKey;
        this.requestedGameweekId = requestedGameweekId;
        this.actualGameweekId = actualGameweekId;
        this.errorCode = "PRIVATE_SESSION_GAMEWEEK_MISMATCH";
    }

    public String getAccessKey() {
        return accessKey;
    }

    public Long getRequestedGameweekId() {
        return requestedGameweekId;
    }

    public Long getActualGameweekId() {
        return actualGameweekId;
    }

    public String getErrorCode() {
        return errorCode;
    }
}