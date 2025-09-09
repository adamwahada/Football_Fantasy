package FootballFantasy.fantasy.Entities.Chat;

public  class CreateSupportTicketRequest {
    private SupportType supportType;
    private String subject;
    private String description;
    private TicketPriority priority = TicketPriority.MEDIUM;

    // Constructeurs
    public CreateSupportTicketRequest() {}

    public CreateSupportTicketRequest(SupportType supportType, String subject,
                                      String description, TicketPriority priority) {
        this.supportType = supportType;
        this.subject = subject;
        this.description = description;
        this.priority = priority != null ? priority : TicketPriority.MEDIUM;
    }

    // Getters et Setters
    public SupportType getSupportType() { return supportType; }
    public void setSupportType(SupportType supportType) { this.supportType = supportType; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public TicketPriority getPriority() { return priority; }
    public void setPriority(TicketPriority priority) { this.priority = priority; }
}