// session-participation.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

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

export type SessionType = 'ONE_VS_ONE' | 'SMALL_GROUP' | 'MEDIUM_GROUP' | 'OPEN_ROOM';

export interface SessionParticipation {
  id: number;
  gameweekId: number;
  sessionId: number;
  userId: number;
  competition: LeagueTheme;
  sessionType: SessionType;
  buyInAmount: number;
  isPrivate: boolean;
  accessKey?: string;
  joinedAt: Date;
  correctPredictions: number;
  totalPredictions: number;
  hasCompletedAll: boolean;
}

export interface UserSessionStats {
  totalSessions: number;
  activeSessions: number;
  completedSessions: number;
  totalWinnings: number;
  winRate: number;
}

export interface EligibilityResponse {
  canJoin: boolean;
}

export interface WinningsResponse {
  totalWinnings: number;
}

@Injectable({
  providedIn: 'root'
})
export class SessionParticipationService {
  private apiUrl = 'http://localhost:9090/fantasy/api/session-participation';
  private adminApiUrl = 'http://localhost:9090/fantasy/api/admin';


  constructor(private http: HttpClient) {}

  /**
   * Join or create a competition session
   */
  joinCompetition(
    gameweekId: number,
    competition: LeagueTheme,
    sessionType: SessionType,
    buyInAmount: number,
    isPrivate: boolean = false,
    accessKey?: string,
    privateMode?: 'CREATE' | 'JOIN'
  ): Observable<SessionParticipation> {
    let params = new HttpParams()
      .set('gameweekId', gameweekId.toString())
      .set('competition', competition)
      .set('sessionType', sessionType)
      .set('buyInAmount', buyInAmount.toString())
      .set('isPrivate', isPrivate.toString());

    if (accessKey) {
      params = params.set('accessKey', accessKey);
    }

    if (privateMode) {
      params = params.set('privateMode', privateMode);
    }

    return this.http.post<SessionParticipation>(`${this.apiUrl}/join-competition`, null, { params });
  }

  /**
   * Join an existing session by ID
   */
  joinSession(sessionId: number): Observable<SessionParticipation> {
    return this.http.post<SessionParticipation>(`${this.apiUrl}/join-session/${sessionId}`, null);
  }

  // ===== LEAVE SESSION =====

  /**
   * Leave a session
   */
  leaveSession(sessionId: number): Observable<string> {
    return this.http.delete(`${this.apiUrl}/leave-session/${sessionId}`, { responseType: 'text' });
  }

  // ===== UPDATE PREDICTION PROGRESS =====

  /**
   * Update user's prediction progress
   */
  updatePredictionProgress(
    participationId: number,
    correctPredictions: number,
    totalPredictions: number,
    hasCompletedAll: boolean
  ): Observable<string> {
    const params = new HttpParams()
      .set('correctPredictions', correctPredictions.toString())
      .set('totalPredictions', totalPredictions.toString())
      .set('hasCompletedAll', hasCompletedAll.toString());

    return this.http.put(`${this.apiUrl}/update-prediction-progress/${participationId}`, null, { 
      params, 
      responseType: 'text' 
    });
  }

  // ===== GET USER PARTICIPATION =====

  /**
   * Get user's participation in a specific session
   */
  getUserParticipation(sessionId: number): Observable<SessionParticipation> {
    return this.http.get<SessionParticipation>(`${this.apiUrl}/user-participation/${sessionId}`);
  }

  /**
   * Get user's participations for a gameweek
   */
  getUserParticipationsForGameweek(gameweekId: number): Observable<SessionParticipation[]> {
    return this.http.get<SessionParticipation[]>(`${this.apiUrl}/user-participations/gameweek/${gameweekId}`);
  }

  /**
   * Get user's active participations
   */
  getUserActiveParticipations(): Observable<SessionParticipation[]> {
    return this.http.get<SessionParticipation[]>(`${this.apiUrl}/user-participations/active`);
  }

  // ===== GET SESSION PARTICIPATIONS =====

  /**
   * Get all participations for a session
   */
  getSessionParticipations(sessionId: number): Observable<SessionParticipation[]> {
    return this.http.get<SessionParticipation[]>(`${this.apiUrl}/session-participations/${sessionId}`);
  }

  // ===== CHECK USER ELIGIBILITY =====

  /**
   * Check if user can join a session type for a gameweek
   */
  canUserJoinSession(
    gameweekId: number,
    sessionType: SessionType,
    buyInAmount: number,
    competition: LeagueTheme
  ): Observable<EligibilityResponse> {
    const params = new HttpParams()
      .set('gameweekId', gameweekId.toString())
      .set('sessionType', sessionType)
      .set('buyInAmount', buyInAmount.toString())
      .set('competition', competition);

    return this.http.get<EligibilityResponse>(`${this.apiUrl}/can-join-session`, { params });
  }

  // ===== USER STATISTICS =====

  /**
   * Get user's session statistics
   */
  getUserSessionStats(): Observable<UserSessionStats> {
    return this.http.get<UserSessionStats>(`${this.apiUrl}/user-stats`);
  }

  /**
   * Get user's total winnings
   */
  getUserTotalWinnings(): Observable<WinningsResponse> {
    return this.http.get<WinningsResponse>(`${this.apiUrl}/user-total-winnings`);
  }

  // ===== ADMIN ENDPOINTS =====

  /**
   * Get user's session statistics (Admin only)
   */
  getUserSessionStatsAdmin(userId: number): Observable<UserSessionStats> {
    return this.http.get<UserSessionStats>(`${this.apiUrl}/admin/user-stats/${userId}`);
  }
  // ===================== CANCEL SESSION WITH AUTOMATIC REFUNDS =====================
  cancelSessionWithRefunds(sessionId: number): Observable<string> {
    const url = `${this.adminApiUrl}/sessions/${sessionId}/cancel-with-refunds`;
    return this.http.post(url, '', { responseType: 'text' });
  }

  // ===================== CANCEL SESSION MANUALLY =====================
  cancelSessionManually(sessionId: number): Observable<string> {
    const url = `${this.adminApiUrl}/sessions/${sessionId}/cancel-manually`;
    return this.http.post(url, '', { responseType: 'text' });
  }

  // ===================== REFUND CANCELLED SESSION LATER =====================
  refundCancelledSession(sessionId: number): Observable<string> {
    const url = `${this.adminApiUrl}/sessions/${sessionId}/refund-cancelled`;
    return this.http.post(url, '', { responseType: 'text' });
  }

}