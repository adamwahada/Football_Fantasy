import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UserEntity } from './user.model';


export enum BanCause {
  CHEATING = 'CHEATING',
  SPAM = 'SPAM',
  HARASSMENT = 'HARASSMENT',
  INAPPROPRIATE_CONTENT = 'INAPPROPRIATE_CONTENT',
  MULTIPLE_ACCOUNTS = 'MULTIPLE_ACCOUNTS',
  PAYMENT_FRAUD = 'PAYMENT_FRAUD',
  SECURITY_THREAT = 'SECURITY_THREAT',
  VIOLATION_OF_RULES = 'VIOLATION_OF_RULES',
  OTHER = 'OTHER'
}
@Injectable({
  providedIn: 'root'
})
export class AdminService {

  private adminApiUrl = 'http://localhost:9090/fantasy/api/admin';

  constructor(private http: HttpClient) { }

  getAllUsers(): Observable<UserEntity[]> {
    return this.http.get<UserEntity[]>(`${this.adminApiUrl}/users`);
  }

creditUserBalance(userId: number, amount: number, adminId: number): Observable<string> {
  const params = new HttpParams().set('amount', amount.toString());
  return this.http.post(`${this.adminApiUrl}/users/${userId}/credit/${adminId}`, null, { responseType: 'text', params });
}

debitUserBalance(userId: number, amount: number, adminId: number): Observable<string> {
  const params = new HttpParams().set('amount', amount.toString());
  return this.http.post(`${this.adminApiUrl}/users/${userId}/debit/${adminId}`, null, { responseType: 'text', params });
}

banUserTemporarily(userId: number, days: number , adminId: number, reason: BanCause): Observable<string> {
  const params = new HttpParams().set('days', days.toString());
  const headers = { 'Content-Type': 'application/json' };
  return this.http.post(
    `${this.adminApiUrl}/users/${userId}/ban-temporary/${adminId}`,
    JSON.stringify(reason),
    { responseType: 'text', params, headers }
  );
}

banUserPermanently(userId: number, adminId: number, reason: BanCause): Observable<string> {
  const headers = { 'Content-Type': 'application/json' };
  return this.http.post(
    `${this.adminApiUrl}/users/${userId}/ban-permanent/${adminId}`,
    JSON.stringify(reason),
    { responseType: 'text', headers }
  );
}

unbanUser(userId: number, adminId: number): Observable<string> {
  return this.http.post(`${this.adminApiUrl}/users/${userId}/unban/${adminId}`, null, { responseType: 'text' });
}

  getUserBanStatus(userId: number): Observable<string> {
    return this.http.get(`${this.adminApiUrl}/users/${userId}/ban-status`, { responseType: 'text' });
  }
}
