package FootballFantasy.fantasy.Entities.GameweekEntities;

public enum LeagueTheme {
    PREMIER_LEAGUE("PL", true),
    SERIE_A("SA", true),
    CHAMPIONS_LEAGUE("CL", true),
    EUROPA_LEAGUE("EL", true),
    BUNDESLIGA("BL1", true),
    LA_LIGA("PD", true),
    LIGUE_ONE("FL1", true),

    // Custom / non-API leagues
    BESTOFF("BO", false),
    CONFERENCE_LEAGUE("CLG", false);

    private final String apiCode;
    private final boolean apiAvailable;

    LeagueTheme(String apiCode, boolean apiAvailable) {
        this.apiCode = apiCode;
        this.apiAvailable = apiAvailable;
    }

    public boolean isApiAvailable() {
        return apiAvailable;
    }

    public String getApiCode() {
        return apiCode;
    }
}
