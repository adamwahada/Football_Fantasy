import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Match, MatchWithIconsDTO } from '../match/match.service';

export type LeagueTheme =
  | 'PREMIER_LEAGUE'
  | 'SERIE_A'
  | 'CHAMPIONS_LEAGUE'
  | 'EUROPA_LEAGUE'
  | 'BUNDESLIGA'          
  | 'LA_LIGA'
  | 'LIGUE_ONE'
  | 'BESTOFF'
  | 'CONFERENCE_LEAGUE';
export interface Gameweek {
  id?: number;
  weekNumber: number;
  status: string;
  competition: LeagueTheme ;
  startDate: string;
  endDate: string;
  joinDeadline: string;
  description?: string;
  tiebreakerMatchIds?: string;
  matches?: Match[];
}
@Injectable({
  providedIn: 'root'
})
export class GameweekService {
  private apiUrl = 'http://localhost:9090/fantasy/api/gameweeks';

  constructor(private http: HttpClient) {}

  // ✅ GET all gameweeks
  getAllGameweeks(): Observable<Gameweek[]> {
    return this.http.get<Gameweek[]>(this.apiUrl);
  }

  // ✅ GET gameweek by ID
  getGameweekById(id: number): Observable<Gameweek> {
    return this.http.get<Gameweek>(`${this.apiUrl}/${id}`);
  }

  // ✅ GET gameweek by week number
  getByWeekNumber(weekNumber: number): Observable<Gameweek> {
    return this.http.get<Gameweek>(`${this.apiUrl}/week/${weekNumber}`);
  }

  // ✅ GET matches in a gameweek
  getMatchesByGameweek(id: number): Observable<Match[]> {
    return this.http.get<Match[]>(`${this.apiUrl}/${id}/matches`);
  }

  // ✅ GET matches with team icons
  getMatchesWithIcons(gameweekId: number): Observable<MatchWithIconsDTO[]> {
    return this.http.get<MatchWithIconsDTO[]>(`${this.apiUrl}/${gameweekId}/matches-with-icons`);
  }

  // ✅ POST create new gameweek
  createGameweek(gameweek: Gameweek): Observable<Gameweek> {
    return this.http.post<Gameweek>(this.apiUrl, gameweek);
  }

  // ✅ PUT update gameweek
  updateGameweek(id: number, gameweek: Gameweek): Observable<Gameweek> {
    return this.http.put<Gameweek>(`${this.apiUrl}/${id}`, gameweek);
  }

  // ✅ DELETE gameweek
  deleteGameweek(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // ✅ POST add match to gameweek
  addMatchToGameweek(gameweekId: number, match: Match): Observable<Match> {
    return this.http.post<Match>(`${this.apiUrl}/${gameweekId}/matches`, match);
  }

  // ✅ PUT link existing match to gameweek
  linkExistingMatch(gameweekId: number, matchId: number): Observable<Match> {
    return this.http.put<Match>(`${this.apiUrl}/${gameweekId}/matches/${matchId}`, {});
  }

  // ✅ PUT link multiple existing matches
  linkMultipleMatches(gameweekId: number, matchIds: number[]): Observable<Match[]> {
    return this.http.put<Match[]>(`${this.apiUrl}/${gameweekId}/matches`, matchIds);
  }

  // ✅ DELETE all matches in gameweek
  deleteAllMatchesFromGameweek(gameweekId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${gameweekId}/matches`);
  }

  // ✅ DELETE selected matches from gameweek
  deleteSelectedMatches(gameweekId: number, matchIds: number[]): Observable<void> {
    return this.http.request<void>('delete', `${this.apiUrl}/${gameweekId}/RemoveMatches`, {
      body: matchIds
    });
  }

  // ✅ Check if gameweek is complete
  isGameweekComplete(gameweekId: number): Observable<boolean> {
    return this.http.get<boolean>(`${this.apiUrl}/${gameweekId}/is-complete`);
  }

  // ✅ Import full matches list to gameweek
  importMatches(gameweekId: number, matches: Match[]): Observable<string> {
    return this.http.post<string>(`${this.apiUrl}/${gameweekId}/import-matches`, matches);
  }

  // ✅ Update global matches
  updateMatchesGlobally(matches: Match[]): Observable<any> {
    return this.http.put(`${this.apiUrl}/matches/update-globally`, matches);
  }

  // ✅ Get matches by competition and week
  getMatchesByCompetition(competition: string, weekNumber: number): Observable<Match[]> {
    const params = new HttpParams()
      .set('competition', competition)
      .set('weekNumber', weekNumber);
    return this.http.get<Match[]>(`${this.apiUrl}/matches-by-competition`, { params });
  }
  // ✅ Get all gameweeks by competition
  getAllGameweeksByCompetition(competition: string): Observable<Gameweek[]> {
  const params = new HttpParams().set('competition', competition);
  return this.http.get<Gameweek[]>(`${this.apiUrl}/all`, { params });
}

  // ✅ Get upcoming gameweeks
  getUpcomingGameweeks(competition: string): Observable<Gameweek[]> {
    const params = new HttpParams().set('competition', competition);
    return this.http.get<Gameweek[]>(`${this.apiUrl}/upcoming`, { params });
  }

  // ✅ Set tiebreakers
  setTiebreakers(gameweekId: number, matchIds: number[]): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${gameweekId}/tiebreakers`, matchIds);
  }

  // ✅ Update tiebreakers
  updateTiebreakers(gameweekId: number, matchIds: number[]): Observable<string> {
    return this.http.put<string>(`${this.apiUrl}/gameweek/${gameweekId}/update-tiebreakers`, matchIds);
  }

  // ✅ Get tiebreaker matches
  getTiebreakerMatches(gameweekId: number): Observable<Match[]> {
    return this.http.get<Match[]>(`${this.apiUrl}/${gameweekId}/tiebreaker-matches`);
  }

  /*** ✅ ADD this new method for flexible gameweek creation
 * Creates match with automatic gameweek creation/adaptation */
addMatchToGameweekFlexible(competition: string, weekNumber: number, match: Match): Observable<Match> {
  const params = new HttpParams()
    .set('competition', competition)
    .set('weekNumber', weekNumber.toString());
    
  return this.http.post<Match>(`${this.apiUrl}/matches`, match, { params });
}
getMatchesCount(gameweekId: number): Observable<number> {
  return this.http.get<number>(`${this.apiUrl}/${gameweekId}/matches-count`);
}

// ✅ GET count of tiebreaker matches
getTiebreakerCount(gameweekId: number): Observable<number> {
  return this.http.get<number>(`${this.apiUrl}/${gameweekId}/tiebreaker-count`);
}
deleteTiebreakerMatches(gameweekId: number, matchIds: number[]): Observable<void> {
  return this.http.request<void>('delete', `${this.apiUrl}/gameweeks/${gameweekId}/tiebreakers`, {
    body: matchIds
  });
}
}
