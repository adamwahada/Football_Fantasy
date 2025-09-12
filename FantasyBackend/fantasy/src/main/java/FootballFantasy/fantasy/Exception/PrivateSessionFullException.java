package FootballFantasy.fantasy.Exception;

public class PrivateSessionFullException extends RuntimeException {
    private final String accessKey;
    private final Long sessionId;
    private final int currentParticipants;
    private final int maxParticipants;
    private final String errorCode;

    public PrivateSessionFullException(String accessKey, Long sessionId, int currentParticipants, int maxParticipants) {
        super(String.format("Private session with access key '%s' is full (%d/%d participants)",
                accessKey, currentParticipants, maxParticipants));
        this.accessKey = accessKey;
        this.sessionId = sessionId;
        this.currentParticipants = currentParticipants;
        this.maxParticipants = maxParticipants;
        this.errorCode = "PRIVATE_SESSION_FULL";
    }

    public String getAccessKey() {
        return accessKey;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public int getCurrentParticipants() {
        return currentParticipants;
    }

    public int getMaxParticipants() {
        return maxParticipants;
    }

    public String getErrorCode() {
        return errorCode;
    }
}