import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UserEntity } from './user.model'; 

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

banUserTemporarily(userId: number, days: number, adminId: number): Observable<string> {
  const params = new HttpParams().set('days', days.toString());
  return this.http.post(`${this.adminApiUrl}/users/${userId}/ban-temporary/${adminId}`, null, { responseType: 'text', params });
}

banUserPermanently(userId: number, adminId: number): Observable<string> {
  return this.http.post(`${this.adminApiUrl}/users/${userId}/ban-permanent/${adminId}`, null, { responseType: 'text' });
}

unbanUser(userId: number, adminId: number): Observable<string> {
  return this.http.post(`${this.adminApiUrl}/users/${userId}/unban/${adminId}`, null, { responseType: 'text' });
}

  getUserBanStatus(userId: number): Observable<string> {
    return this.http.get(`${this.adminApiUrl}/users/${userId}/ban-status`, { responseType: 'text' });
  }
}
