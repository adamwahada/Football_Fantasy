package FootballFantasy.fantasy.Entities.Chat;

public enum SupportStatus {
    OPEN("Ouvert"),
    IN_PROGRESS("En cours"),
    RESOLVED("Résolu"),
    CLOSED("Fermé");

    private String displayName;

    SupportStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}