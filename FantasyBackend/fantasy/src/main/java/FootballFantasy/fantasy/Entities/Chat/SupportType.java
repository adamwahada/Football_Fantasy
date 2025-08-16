package FootballFantasy.fantasy.Entities.Chat;

public enum SupportType {
    PAYMENT("Paiement"),
    TECHNICAL("Problème technique"),
    ACCOUNT("Compte/Profil"),
    GENERAL("Question générale");

    private String displayName;

    SupportType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}