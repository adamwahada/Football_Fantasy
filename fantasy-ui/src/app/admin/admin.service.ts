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

  creditUserBalance(userId: number, amount: number): Observable<string> {
    const params = new HttpParams().set('amount', amount.toString());
    return this.http.post(`${this.adminApiUrl}/users/${userId}/credit`, null, { responseType: 'text', params });
  }

  debitUserBalance(userId: number, amount: number): Observable<string> {
    const params = new HttpParams().set('amount', amount.toString());
    return this.http.post(`${this.adminApiUrl}/users/${userId}/debit`, null, { responseType: 'text', params });
  }

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
