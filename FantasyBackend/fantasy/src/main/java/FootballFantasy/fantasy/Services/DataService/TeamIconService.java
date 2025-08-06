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
        initializeOtherLeagueTeams();
    }

    private void initializeLeagueIcons() {
        leagueIcons.put("Premier League", "/assets/images/leagues/premier-league.png");
        leagueIcons.put("La Liga", "/assets/images/leagues/la-liga.png");
        leagueIcons.put("Serie A", "/assets/images/leagues/serie-a.png");
        leagueIcons.put("Bundesliga", "/assets/images/leagues/bundesliga.png");
        leagueIcons.put("Ligue 1", "/assets/images/leagues/ligue1.png");
        leagueIcons.put("Eredivisie", "/assets/images/leagues/eredivisie.png");
        leagueIcons.put("Primeira Liga", "/assets/images/leagues/primeira-liga.png");
        leagueIcons.put("Belgian Pro League", "/assets/images/leagues/belgian-pro-league.png");
        leagueIcons.put("Super Lig", "/assets/images/leagues/super-lig.png");
        leagueIcons.put("Czech First League", "/assets/images/leagues/czech-first-league.png");
        leagueIcons.put("Champions League", "/assets/images/leagues/champions-league.png");
        leagueIcons.put("Europa League", "/assets/images/leagues/europa-league.png");
    }

    private void initializePremierLeagueTeams() {
        String league = "Premier League";
        addTeam("Arsenal", league, "/assets/images/teams/arsenal.png");
        addTeam("Aston Villa", league, "/assets/images/teams/aston-villa.png");
        addTeam("Brentford", league, "/assets/images/teams/brentford.png");
        addTeam("Brighton and Hove Albion", league, "/assets/images/teams/brighton.png");
        addTeam("Burnley", league, "/assets/images/teams/burnley.png");
        addTeam("Chelsea", league, "/assets/images/teams/chelsea.png");
        addTeam("Crystal Palace", league, "/assets/images/teams/crystal-palace.png");
        addTeam("Everton", league, "/assets/images/teams/everton.png");
        addTeam("Fulham", league, "/assets/images/teams/fulham.png");
        addTeam("Liverpool", league, "/assets/images/teams/liverpool.png");
        addTeam("Leeds United", league, "/assets/images/teams/leeds.png");
        addTeam("Manchester City", league, "/assets/images/teams/manchester-city.png");
        addTeam("Manchester United", league, "/assets/images/teams/manchester-united.png");
        addTeam("Newcastle United", league, "/assets/images/teams/newcastle.png");
        addTeam("Nottingham Forest", league, "/assets/images/teams/nottingham-forest.png");
        addTeam("Sunderland", league, "/assets/images/teams/sunderland.png");
        addTeam("Tottenham Hotspur", league, "/assets/images/teams/tottenham.png");
        addTeam("West Ham United", league, "/assets/images/teams/west-ham.png");
        addTeam("Wolverhampton Wanderers", league, "/assets/images/teams/wolves.png");
        addTeam("Bournemouth", league, "/assets/images/teams/bournemouth.png");
    }

    private void initializeSerieATeams() {
        String league = "Serie A";
        addTeam("AC Milan", league, "/assets/images/teams/ac-milan.png");
        addTeam("Atalanta", league, "/assets/images/teams/atalanta.png");
        addTeam("Bologna", league, "/assets/images/teams/bologna.png");
        addTeam("Cagliari", league, "/assets/images/teams/cagliari.png");
        addTeam("Como", league, "/assets/images/teams/como.png");
        addTeam("Cremonese", league, "/assets/images/teams/cremonese.png");
        addTeam("Fiorentina", league, "/assets/images/teams/fiorentina.png");
        addTeam("Genoa", league, "/assets/images/teams/genoa.png");
        addTeam("Inter", league, "/assets/images/teams/inter-milan.png");
        addTeam("Juventus", league, "/assets/images/teams/juventus.png");
        addTeam("Lazio", league, "/assets/images/teams/lazio.png");
        addTeam("Lecce", league, "/assets/images/teams/lecce.png");
        addTeam("Napoli", league, "/assets/images/teams/napoli.png");
        addTeam("Parma", league, "/assets/images/teams/parma.png");
        addTeam("Pisa", league, "/assets/images/teams/pisa.png");
        addTeam("Roma", league, "/assets/images/teams/roma.png");
        addTeam("Sassuolo", league, "/assets/images/teams/sassuolo.png");
        addTeam("Torino", league, "/assets/images/teams/torino.png");
        addTeam("Udinese", league, "/assets/images/teams/udinese.png");
        addTeam("Hellas Verona", league, "/assets/images/teams/hellas-verona.png");
    }

    private void initializeLaLigaTeams() {
        String league = "La Liga";

        addTeam("Deportivo Alaves", league, "/assets/images/teams/alaves.png");
        addTeam("Ath. Bilbao", league, "/assets/images/teams/athletic-bilbao.png");
        addTeam("Atletico Madrid", league, "/assets/images/teams/atletico-madrid.png");
        addTeam("Barcelona", league, "/assets/images/teams/barcelona.png");
        addTeam("Celta Vigo", league, "/assets/images/teams/celta-vigo.png");
        addTeam("Elche", league, "/assets/images/teams/elche.png");
        addTeam("Espanyol", league, "/assets/images/teams/espanyol.png");
        addTeam("Getafe", league, "/assets/images/teams/getafe.png");
        addTeam("Girona", league, "/assets/images/teams/girona.png");
        addTeam("Levante", league, "/assets/images/teams/levante.png");
        addTeam("Mallorca", league, "/assets/images/teams/mallorca.png");
        addTeam("Osasuna", league, "/assets/images/teams/osasuna.png");
        addTeam("Rayo Vallecano", league, "/assets/images/teams/rayo-vallecano.png");
        addTeam("Real Betis", league, "/assets/images/teams/real-betis.png");
        addTeam("Real Madrid", league, "/assets/images/teams/real-madrid.png");
        addTeam("Real Oviedo", league, "/assets/images/teams/real-oviedo.png");
        addTeam("Real Sociedad", league, "/assets/images/teams/real-sociedad.png");
        addTeam("Seville", league, "/assets/images/teams/sevilla.png");
        addTeam("Valencia", league, "/assets/images/teams/valencia.png");
        addTeam("Villarreal", league, "/assets/images/teams/villarreal.png");
    }

    private void initializeLigue1Teams() {
        String league = "Ligue 1";
        addTeam("Angers", league, "/assets/images/teams/angers.png");
        addTeam("Auxerre", league, "/assets/images/teams/auxerre.png");
        addTeam("Brest", league, "/assets/images/teams/brest.png");
        addTeam("Le Havre", league, "/assets/images/teams/le-havre.png");
        addTeam("Lens", league, "/assets/images/teams/lens.png");
        addTeam("Lille", league, "/assets/images/teams/lille.png");
        addTeam("Lorient", league, "/assets/images/teams/lorient.png");
        addTeam("Olympique de Lyon", league, "/assets/images/teams/lyon.png");
        addTeam("Olympique de Marseille", league, "/assets/images/teams/marseille.png");
        addTeam("Metz", league, "/assets/images/teams/metz.png");
        addTeam("Monaco", league, "/assets/images/teams/monaco.png");
        addTeam("Nantes", league, "/assets/images/teams/nantes.png");
        addTeam("Nice", league, "/assets/images/teams/nice.png");
        addTeam("Paris FC", league, "/assets/images/teams/paris-fc.png");
        addTeam("Paris Saint-Germain", league, "/assets/images/teams/psg.png");
        addTeam("Rennes", league, "/assets/images/teams/rennes.png");
        addTeam("Strasbourg", league, "/assets/images/teams/strasbourg.png");
        addTeam("Toulouse", league, "/assets/images/teams/toulouse.png");
    }
    private void initializeBundesligaTeams() {
        String league = "Bundesliga";
        addTeam("Augsburg", league, "/assets/images/teams/augsburg.png");
        addTeam("Bayer Leverkusen", league, "/assets/images/teams/bayer-leverkusen.png");
        addTeam("Bayern Munich", league, "/assets/images/teams/bayern-munich.png");
        addTeam("Borussia Dortmund", league, "/assets/images/teams/borussia-dortmund.png");
        addTeam("Borussia Mönchengladbach", league, "/assets/images/teams/monchengladbach.png");
        addTeam("Eintracht Frankfurt", league, "/assets/images/teams/eintracht-frankfurt.png");
        addTeam("Freiburg", league, "/assets/images/teams/freiburg.png");
        addTeam("Heidenheim", league, "/assets/images/teams/heidenheim.png");
        addTeam("Hoffenheim", league, "/assets/images/teams/hoffenheim.png");
        addTeam("Koln", league, "/assets/images/teams/koln.png");
        addTeam("Mainz", league, "/assets/images/teams/mainz.png");
        addTeam("RB Leipzig", league, "/assets/images/teams/rb-leipzig.png");
        addTeam("Stuttgart", league, "/assets/images/teams/stuttgart.png");
        addTeam("Union Berlin", league, "/assets/images/teams/union-berlin.png");
        addTeam("Werder Bremen", league, "/assets/images/teams/werder-bremen.png");
        addTeam("Wolfsburg", league, "/assets/images/teams/wolfsburg.png");
        addTeam("Hamburg", league, "/assets/images/teams/hamburg.png");
        addTeam("St Pauli", league, "/assets/images/teams/pauli.png");
    }

    private void initializeOtherLeagueTeams() {
        // Eredivisie (Netherlands)
        addTeam("PSV Eindhoven", "Eredivisie", "/assets/images/teams/psv-eindhoven.png");
        addTeam("Ajax Amsterdam", "Eredivisie", "/assets/images/teams/ajax-amsterdam.png");

        // Primeira Liga (Portugal)
        addTeam("Sporting CP", "Primeira Liga", "/assets/images/teams/sporting-cp.png");

        // Belgian Pro League
        addTeam("Union SG", "Belgian Pro League", "/assets/images/teams/union-sg.png");

        // Super Lig (Turkey)
        addTeam("Galatasaray SK", "Super Lig", "/assets/images/teams/galatasaray.png");

        // Czech First League
        addTeam("Slavia Prague", "Czech First League", "/assets/images/teams/slavia-prague.png");
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

    // Get all teams with their icons
    public Map<String, String> getAllTeamIcons() {
        return new HashMap<>(teamIcons);
    }

    // Get teams by league
    public Map<String, String> getTeamsByLeague(String leagueName) {
        Map<String, String> leagueTeams = new HashMap<>();
        for (Map.Entry<String, String> entry : teamToLeague.entrySet()) {
            if (entry.getValue().equals(leagueName)) {
                leagueTeams.put(entry.getKey(), teamIcons.get(entry.getKey()));
            }
        }
        return leagueTeams;
    }
}