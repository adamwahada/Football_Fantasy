package FootballFantasy.fantasy.Exception;

public class PrivateSessionGameweekMismatchException extends RuntimeException {
    private final String accessKey;
    private final Long requestedGameweekId;
    private final Long actualGameweekId;
    private final String errorCode;

    public PrivateSessionGameweekMismatchException(String accessKey, Long requestedGameweekId, Long actualGameweekId) {
        super(String.format("Cette clé d'accès '%s' est pour la gameweek %d, mais vous essayez de rejoindre la gameweek %d",
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