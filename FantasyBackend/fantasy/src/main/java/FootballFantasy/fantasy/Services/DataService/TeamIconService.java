package FootballFantasy.fantasy.Services.DataService;

import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class TeamIconService {

    private final Map<String, String> teamIcons = new HashMap<>();
    private final Map<String, String> leagueIcons = new HashMap<>();
    private final Map<String, String> teamToLeague = new HashMap<>();

    public TeamIconService() {
        initializeLeagueIcons();
        initializePremierLeagueTeams();
        initializeLaLigaTeams();
        initializeSerieATeams();
        initializeBundesligaTeams();
        initializeLigue1Teams();
    }

    private void initializeLeagueIcons() {
        leagueIcons.put("Premier League", "/assets/images/leagues/premier-league.png");
        leagueIcons.put("La Liga", "/assets/images/leagues/la-liga.png");
        leagueIcons.put("Serie A", "/assets/images/leagues/serie-a.png");
        leagueIcons.put("Bundesliga", "/assets/images/leagues/bundesliga.png");
        leagueIcons.put("Ligue 1", "/assets/images/leagues/ligue1.png");
        leagueIcons.put("Champions League", "/assets/images/leagues/champions-league.png");
        leagueIcons.put("Europa League", "/assets/images/leagues/europa-league.png");
    }

    private void initializePremierLeagueTeams() {
        String league = "Premier League";
        addTeam("Liverpool", league, "/assets/images/teams/liverpool.png");
        addTeam("Manchester City", league, "/assets/images/teams/manchester-city.png");
        addTeam("Chelsea", league, "/assets/images/teams/chelsea.png");
        addTeam("Arsenal", league, "/assets/images/teams/arsenal.png");
        addTeam("Manchester United", league, "/assets/images/teams/manchester-united.png");
        addTeam("Tottenham Hotspur", league, "/assets/images/teams/tottenham.png");
        addTeam("Newcastle United", league, "/assets/images/teams/newcastle.png");
        addTeam("Aston Villa", league, "/assets/images/teams/aston-villa.png");
        addTeam("Brighton and Hove Albion", league, "/assets/images/teams/brighton.png");
        addTeam("West Ham United", league, "/assets/images/teams/west-ham.png");
        addTeam("Crystal Palace", league, "/assets/images/teams/crystal-palace.png");
        addTeam("Fulham", league, "/assets/images/teams/fulham.png");
        addTeam("Bournemouth", league, "/assets/images/teams/bournemouth.png");
        addTeam("Wolverhampton Wanderers", league, "/assets/images/teams/wolves.png");
        addTeam("Everton", league, "/assets/images/teams/everton.png");
        addTeam("Brentford", league, "/assets/images/teams/brentford.png");
        addTeam("Nottingham Forest", league, "/assets/images/teams/nottingham-forest.png");
        addTeam("Leeds United", league, "/assets/images/teams/leeds.png");
        addTeam("Sunderland", league, "/assets/images/teams/sunderland.png");
        addTeam("Burnley", league, "/assets/images/teams/burnley.png");
    }

    private void initializeLaLigaTeams() {
        String league = "La Liga";
        addTeam("Real Madrid", league, "/assets/images/teams/real-madrid.png");
        addTeam("Barcelona", league, "/assets/images/teams/barcelona.png");
        addTeam("Atletico Madrid", league, "/assets/images/teams/atletico-madrid.png");
        addTeam("Sevilla", league, "/assets/images/teams/sevilla.png");
        addTeam("Real Betis", league, "/assets/images/teams/real-betis.png");
        addTeam("Valencia", league, "/assets/images/teams/valencia.png");
        addTeam("Villarreal", league, "/assets/images/teams/villarreal.png");
        addTeam("Athletic Bilbao", league, "/assets/images/teams/athletic-bilbao.png");
        addTeam("Real Sociedad", league, "/assets/images/teams/real-sociedad.png");
        addTeam("Celta Vigo", league, "/assets/images/teams/celta-vigo.png");
    }

    private void initializeSerieATeams() {
        String league = "Serie A";
        addTeam("Juventus", league, "/assets/images/teams/juventus.png");
        addTeam("AC Milan", league, "/assets/images/teams/ac-milan.png");
        addTeam("Inter Milan", league, "/assets/images/teams/inter-milan.png");
        addTeam("Napoli", league, "/assets/images/teams/napoli.png");
        addTeam("AS Roma", league, "/assets/images/teams/roma.png");
        addTeam("Lazio", league, "/assets/images/teams/lazio.png");
        addTeam("Atalanta", league, "/assets/images/teams/atalanta.png");
        addTeam("Fiorentina", league, "/assets/images/teams/fiorentina.png");
        addTeam("Torino", league, "/assets/images/teams/torino.png");
        addTeam("Bologna", league, "/assets/images/teams/bologna.png");
    }

    private void initializeBundesligaTeams() {
        String league = "Bundesliga";
        addTeam("Bayern Munich", league, "/assets/images/teams/bayern-munich.png");
        addTeam("Borussia Dortmund", league, "/assets/images/teams/borussia-dortmund.png");
        addTeam("RB Leipzig", league, "/assets/images/teams/rb-leipzig.png");
        addTeam("Bayer Leverkusen", league, "/assets/images/teams/bayer-leverkusen.png");
        addTeam("Borussia Monchengladbach", league, "/assets/images/teams/monchengladbach.png");
        addTeam("Eintracht Frankfurt", league, "/assets/images/teams/eintracht-frankfurt.png");
        addTeam("VfL Wolfsburg", league, "/assets/images/teams/wolfsburg.png");
        addTeam("Union Berlin", league, "/assets/images/teams/union-berlin.png");
    }

    private void initializeLigue1Teams() {
        String league = "Ligue 1";
        addTeam("Paris Saint-Germain", league, "/assets/images/teams/psg.png");
        addTeam("Marseille", league, "/assets/images/teams/marseille.png");
        addTeam("Lyon", league, "/assets/images/teams/lyon.png");
        addTeam("Monaco", league, "/assets/images/teams/monaco.png");
        addTeam("Lille", league, "/assets/images/teams/lille.png");
        addTeam("Nice", league, "/assets/images/teams/nice.png");
        addTeam("Rennes", league, "/assets/images/teams/rennes.png");
    }

    private void addTeam(String teamName, String league, String iconPath) {
        teamIcons.put(teamName, iconPath);
        teamToLeague.put(teamName, league);
    }

    public String getTeamIcon(String teamName) {
        return teamIcons.getOrDefault(teamName, "/assets/images/teams/default.png");
    }

    public String getLeagueIcon(String leagueName) {
        return leagueIcons.getOrDefault(leagueName, "/assets/images/leagues/default.png");
    }

    // Auto-detect league from team name if not provided
    public String getLeagueFromTeam(String teamName) {
        return teamToLeague.getOrDefault(teamName, "Unknown League");
    }

    // Get all supported leagues
    public Map<String, String> getAllLeagues() {
        return new HashMap<>(leagueIcons);
    }
}