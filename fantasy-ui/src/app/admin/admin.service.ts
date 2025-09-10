import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AdminService {

  private adminApiUrl = 'http://localhost:9090/fantasy/api/admin';

  constructor(private http: HttpClient) { }

  // ---------------- USER BALANCE ----------------
  creditUserBalance(userId: number, amount: number): Observable<string> {
    const params = new HttpParams().set('amount', amount.toString());
    return this.http.post(`${this.adminApiUrl}/users/${userId}/credit`, null, { responseType: 'text', params });
  }

  debitUserBalance(userId: number, amount: number): Observable<string> {
  const params = new HttpParams().set('amount', amount.toString());
  return this.http.post(`${this.adminApiUrl}/users/${userId}/debit`, null, { responseType: 'text', params });
}
  // ---------------- MATCH UPDATES ----------------
//   triggerMatchUpdate(competition?: string): Observable<string> {
//     let params = new HttpParams();
//     if (competition) {
//       params = params.set('competition', competition);
//     }
//     return this.http.post(`${this.adminApiUrl}/matches/update-now`, null, { responseType: 'text', params });
//   }

//   updateSpecificGameweek(competition: string, weekNumber: number): Observable<string> {
//     const params = new HttpParams()
//       .set('competition', competition)
//       .set('weekNumber', weekNumber.toString());
//     return this.http.post(`${this.adminApiUrl}/matches/update-gameweek`, null, { responseType: 'text', params });
//   }

  // ---------------- USER BAN MANAGEMENT ----------------
  banUserTemporarily(userId: number, days: number): Observable<string> {
    const params = new HttpParams().set('days', days.toString());
    return this.http.post(`${this.adminApiUrl}/users/${userId}/ban-temporary`, null, { responseType: 'text', params });
  }

  banUserPermanently(userId: number): Observable<string> {
    return this.http.post(`${this.adminApiUrl}/users/${userId}/ban-permanent`, null, { responseType: 'text' });
  }

  unbanUser(userId: number): Observable<string> {
    return this.http.post(`${this.adminApiUrl}/users/${userId}/unban`, null, { responseType: 'text' });
  }

  getUserBanStatus(userId: number): Observable<string> {
    return this.http.get(`${this.adminApiUrl}/users/${userId}/ban-status`, { responseType: 'text' });
  }
}
