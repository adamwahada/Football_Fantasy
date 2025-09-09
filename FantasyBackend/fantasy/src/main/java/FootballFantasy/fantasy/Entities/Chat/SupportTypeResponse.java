package FootballFantasy.fantasy.Entities.Chat;


public  class SupportTypeResponse {
    private String value;
    private String displayName;

    public SupportTypeResponse(SupportType type, String displayName) {
        this.value = type.name();
        this.displayName = displayName;
    }

    // Getters
    public String getValue() { return value; }
    public String getDisplayName() { return displayName; }
}