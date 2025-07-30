import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Match {
  id?: number;
  homeTeam: string;
  awayTeam: string;
  matchDate: string; // ISO String (backend utilise LocalDateTime)
  homeScore?: number;
  awayScore?: number;
  predictionDeadline?: string;
  finished?: boolean;
  description?: string;
  status: 'SCHEDULED' | 'LIVE' | 'COMPLETED' | 'CANCELED';
  active?: boolean;
  gameweeks?: { id: number }[]; 
}

@Injectable({
  providedIn: 'root',
})
export class MatchService {
  private apiUrl = 'http://localhost:9090/fantasy/api/matches';

  constructor(private http: HttpClient) {}

  // ✅ Récupérer tous les matchs
  getAllMatches(): Observable<Match[]> {
    return this.http.get<Match[]>(this.apiUrl);
  }

  // ✅ Récupérer un match par ID
  getMatchById(id: number): Observable<Match> {
    return this.http.get<Match>(`${this.apiUrl}/${id}`);
  }

  // ✅ Créer un match (ADMIN)
  createMatch(match: Match): Observable<Match> {
    return this.http.post<Match>(this.apiUrl, match);
  }

  // ✅ Mettre à jour un match (ADMIN)
  updateMatch(id: number, match: Match): Observable<Match> {
    return this.http.put<Match>(`${this.apiUrl}/${id}`, match);
  }

  // ✅ Supprimer un match (ADMIN)
  deleteMatch(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // ✅ Obtenir le vainqueur d’un match
  getMatchWinner(id: number): Observable<string> {
    return this.http.get(`${this.apiUrl}/${id}/winner`, { responseType: 'text' });
  }

  // ✅ Activer/désactiver un match
  setMatchActiveStatus(id: number, active: boolean): Observable<string> {
    return this.http.put(`${this.apiUrl}/${id}/active?active=${active}`, null, {
      responseType: 'text',
    });
  }
}
