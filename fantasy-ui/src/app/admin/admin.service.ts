import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UserEntity } from './user.model';

export interface UserManagementAuditDTO {
  id: number;
  userId: number;
  username?: string;
  email?: string;
  adminId: number;
  adminUsername?: string;
  action: UserAction;
  details?: string;
  reason?: BanCause;
  timestamp: string;
  formattedDate?: string;
  amount?: number;
  days?: number;
}


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

export enum UserAction {
  BAN = 'BAN',
  TEMP_BAN = 'TEMP_BAN',
  CREDIT = 'CREDIT',
  DEBIT = 'DEBIT',
  PERMANENT_BAN = 'PERMANENT_BAN',
  UNBAN = 'UNBAN'
}

@Injectable({
  providedIn: 'root'
})
export class AdminService {

  private adminApiUrl = 'http://localhost:9090/fantasy/api/admin';
  private adminmanagerApiUrl = 'http://localhost:9090/fantasy/api/admin-management';

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

   //Keep the history of all admin actions and user actions
   getAllAuditRecords(): Observable<UserManagementAuditDTO[]> {
  return this.http.get<UserManagementAuditDTO[]>(`${this.adminmanagerApiUrl}/audit/all`);
}

  // Full user history
  getUserHistory(userId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.adminApiUrl}/users/${userId}/history`);
  }

  // User history filtered by action
  getUserHistoryByAction(userId: number, action: UserAction): Observable<any[]> {
    return this.http.get<any[]>(`${this.adminApiUrl}/users/${userId}/history/action/${action}`);
  }

  // User history filtered by reason
  getUserHistoryByReason(userId: number, reason: BanCause): Observable<any[]> {
    return this.http.get<any[]>(`${this.adminApiUrl}/users/${userId}/history/reason/${reason}`);
  }

  // All actions by an admin
  getAdminActions(adminId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.adminApiUrl}/admins/${adminId}/actions`);
  }

  // Admin actions filtered by action
  getAdminHistoryByAction(adminId: number, action: UserAction): Observable<any[]> {
    return this.http.get<any[]>(`${this.adminApiUrl}/admins/${adminId}/actions/${action}`);
  }

  // All actions filtered by reason
  getActionsByReason(reason: BanCause): Observable<any[]> {
    return this.http.get<any[]>(`${this.adminApiUrl}/actions/reason/${reason}`);
  }
}
