package FootballFantasy.fantasy.Entities.Chat;

public enum TicketPriority {
    LOW("Basse"),
    MEDIUM("Moyenne"),
    HIGH("Élevée"),
    URGENT("Urgente");

    private final String displayName;

    TicketPriority(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}