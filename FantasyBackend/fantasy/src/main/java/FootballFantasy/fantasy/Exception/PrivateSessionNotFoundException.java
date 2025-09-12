
// PrivateSessionNotFoundException.java
package FootballFantasy.fantasy.Exception;

public class PrivateSessionNotFoundException extends RuntimeException {
    private final String accessKey;
    private final String errorCode;

    public PrivateSessionNotFoundException(String accessKey, String errorCode, String message) {
        super(message);
        this.accessKey = accessKey;
        this.errorCode = errorCode;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getErrorCode() {
        return errorCode;
    }
}