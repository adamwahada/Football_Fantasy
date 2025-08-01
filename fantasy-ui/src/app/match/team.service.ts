import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface TeamIcon {
  name: string;
  iconUrl: string;
  league: string;
}

@Injectable({
  providedIn: 'root'
})
export class TeamService {
  private apiUrl = 'http://localhost:9090/fantasy/api/teams';
  private baseUrl = 'http://localhost:9090/fantasy';

  constructor(private http: HttpClient) { }

  getAllTeamIcons(): Observable<{[key: string]: string}> {
    return this.http.get<{[key: string]: string}>(`${this.apiUrl}/icons`);
  }

  getTeamIcon(teamName: string): Observable<string> {
    return this.http.get<string>(`${this.apiUrl}/${teamName}/icon`);
  }

  // Transform the teams array into TeamIcon objects
  getTeamsWithIcons(teams: string[], teamIcons: {[key: string]: string}): TeamIcon[] {
    return teams.map(team => ({
      name: team,
      iconUrl: this.convertToFullUrl(teamIcons[team] || this.getDefaultIconPath(team)),
      league: this.getLeagueFromTeam(team) 
    }));
  }

getDefaultIconPath(teamName: string): string {
  return '/assets/images/teams/default.png';
}

  // Convertir les chemins relatifs en URLs complètes
private convertToFullUrl(relativePath: string): string {
  if (!relativePath || relativePath.includes('default.png')) {
    return `${this.baseUrl}/assets/images/teams/default.png`;
  }
  
  if (relativePath.startsWith('http')) {
    return relativePath;
  }

  return `${this.baseUrl}${relativePath}`;
}

  private getLeagueFromTeam(teamName: string): string {
    // This is a simple implementation - you could also get this from your backend
    const premierLeagueTeams = ['Arsenal', 'Aston Villa', 'Brentford', 'Brighton & Hove Albion', 'Burnley', 'Chelsea', 'Crystal Palace', 'Everton', 'Fulham', 'Liverpool', 'Leeds United', 'Manchester City', 'Manchester United', 'Newcastle United', 'Nottingham Forest', 'Sunderland', 'Tottenham Hotspur', 'West Ham United', 'Wolverhampton Wanderers', 'Bournemouth'];
    const serieATeams = ['AC Milan', 'Atalanta', 'Bologna', 'Cagliari', 'Como', 'Cremonese', 'Fiorentina', 'Genoa', 'Inter', 'Juventus', 'Lazio', 'Lecce', 'Napoli', 'Parma', 'Pisa', 'Roma', 'Sassuolo', 'Torino', 'Udinese', 'Hellas Verona'];
    const laLigaTeams = ['Alaves', 'Almeria', 'Athletic Club', 'Atlético Madrid', 'Barcelona', 'Cadiz', 'Celta Vigo', 'Getafe', 'Girona', 'Granada', 'Las Palmas', 'Leganés', 'Mallorca', 'Osasuna', 'Rayo Vallecano', 'Real Betis', 'Real Madrid', 'Real Sociedad', 'Sevilla', 'Villarreal'];
    const ligue1Teams = ['Auxerre', 'Brest', 'Clermont', 'Le Havre', 'Lens', 'Lille', 'Lorient', 'Olympique de Lyon', 'Olympique de Marseille', 'Metz', 'Monaco', 'Montpellier', 'Nantes', 'Nice', 'Paris FC', 'Paris Saint-Germain', 'Reims', 'Rennes', 'Strasbourg', 'Toulouse'];
    const bundesligaTeams = ['Augsburg', 'Bayer Leverkusen', 'Bayern Munich', 'Bochum', 'Borussia Dortmund', 'Borussia Mönchengladbach', 'Darmstadt', 'Eintracht Frankfurt', 'Freiburg', 'Heidenheim', 'Hoffenheim', 'Koln', 'Mainz', 'RB Leipzig', 'Schalke 04', 'Stuttgart', 'Union Berlin', 'Werder Bremen', 'Wolfsburg', 'Hamburg'];

    const eredivisieTeams = ['PSV Eindhoven', 'Ajax Amsterdam'];
    const primeiraLigaTeams = ['Sporting CP'];
    const belgianProLeagueTeams = ['Union SG'];
    const superLigTeams = ['Galatasaray SK'];
    const czechFirstLeagueTeams = ['Slavia Prague'];


    if (premierLeagueTeams.includes(teamName)) return 'Premier League';
    if (serieATeams.includes(teamName)) return 'Serie A';
    if (laLigaTeams.includes(teamName)) return 'La Liga';
    if (ligue1Teams.includes(teamName)) return 'Ligue 1';
    if (bundesligaTeams.includes(teamName)) return 'Bundesliga';
    if (eredivisieTeams.includes(teamName)) return 'Eredivisie';
    if (primeiraLigaTeams.includes(teamName)) return 'Primeira Liga';
    if (belgianProLeagueTeams.includes(teamName)) return 'Belgian Pro League';
    if (superLigTeams.includes(teamName)) return 'Super Lig';
    if (czechFirstLeagueTeams.includes(teamName)) return 'Czech First League';
    return 'Other';
  }
}