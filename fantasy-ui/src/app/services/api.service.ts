import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private baseUrl = 'http://localhost:9090/fantasy/api';

  constructor(private http: HttpClient) {}

  // Endpoint for users with 'user' role
  getUserData(): Observable<any> {
    console.log('=== Calling User Endpoint ===');
    const url = `${this.baseUrl}/test`;
    console.log('URL:', url);

    return this.http.get(url, { responseType: 'text' }).pipe(
      tap(response => {
        console.log('=== User Data Response ===');
        console.log('Response:', response);
      }),
      catchError(error => {
        console.error('=== User Data Error ===');
        console.error('Status:', error.status);
        console.error('Message:', error.message);
        console.error('Error:', error);
        return throwError(() => error);
      })
    );
  }

  // Endpoint for users with 'admin' role
  getAdminData(): Observable<any> {
    console.log('=== Calling Admin Endpoint ===');
    const url = `${this.baseUrl}/test3`;
    console.log('URL:', url);

    return this.http.get(url, { responseType: 'text' }).pipe(
      tap(response => {
        console.log('=== Admin Data Response ===');
        console.log('Response:', response);
      }),
      catchError(error => {
        console.error('=== Admin Data Error ===');
        console.error('Status:', error.status);
        console.error('Message:', error.message);
        console.error('Error:', error);
        return throwError(() => error);
      })
    );
  }
}