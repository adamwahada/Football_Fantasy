import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class ReferralCodeService {
  private baseUrl = 'http://localhost:9090/fantasy/api'; 

  constructor(private http: HttpClient) {}

  // 🔍 Get all referral codes (admin only)
  getAllReferralCodes(): Observable<any> {
    const url = `${this.baseUrl}/admin/referral/all`;
    console.log('📥 GET:', url);
    return this.http.get(url).pipe(
      tap(res => console.log('✅ Referral codes:', res)),
      catchError(error => {
        console.error('❌ Failed to fetch referral codes:', error);
        return throwError(() => error);
      })
    );
  }

  // ➕ Create a new referral code
  createReferralCode(code: string, expirationDate?: string): Observable<any> {
    const url = `${this.baseUrl}/admin/referral/create`;
    const payload = { code, expirationDate };
    console.log('📤 POST:', url, payload);

    return this.http.post(url, payload).pipe(
      tap(res => console.log('✅ Referral code created:', res)),
      catchError(error => {
        console.error('❌ Failed to create referral code:', error);
        return throwError(() => error);
      })
    );
  }

  // 🗑️ Delete a referral code by ID or code string
  deleteReferralCode(code: string): Observable<any> {
    const url = `${this.baseUrl}/admin/referral/delete/${code}`;
    console.log('🗑️ DELETE:', url);

    return this.http.delete(url).pipe(
      tap(() => console.log(`✅ Referral code "${code}" deleted`)),
      catchError(error => {
        console.error('❌ Failed to delete referral code:', error);
        return throwError(() => error);
      })
    );
  }
}
