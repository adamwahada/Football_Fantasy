package FootballFantasy.fantasy.Entities.GameweekEntity;

public enum LeagueTheme {
    PREMIER_LEAGUE("PL"),
    SERIE_A("SA"),
    CHAMPIONS_LEAGUE("CL"),
    EUROPA_LEAGUE("EL"),
    BUNDESLIGA("BL1"),
    LA_LIGA("PD"),
    LIGUE_ONE("FL1"),
    BESTOFF("BO"),
    CONFERENCE_LEAGUE("CLG");

    private final String apiCode;

    LeagueTheme(String apiCode) {
        this.apiCode = apiCode;
    }

    public String getApiCode() {
        return apiCode;
    }
}
