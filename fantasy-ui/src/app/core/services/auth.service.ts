// auth.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { KeycloakService } from '../../keycloak.service';
import { Router } from '@angular/router';
import { Observable, BehaviorSubject, of, throwError, from } from 'rxjs';
import { catchError, tap, switchMap } from 'rxjs/operators';
import { map } from 'rxjs/operators';


export interface CurrentUser {
  id: number;           // App DB ID
  keycloakId: string;   // Keycloak subject ID
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  balance: number;      
  termsAccepted: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  
  private registrationApiUrl = 'http://localhost:9090/fantasy/api/user';
  private userApiUrl = 'http://localhost:9090/fantasy/api/users';

  private currentUserSubject = new BehaviorSubject<CurrentUser | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();
  
  constructor(
    public keycloakService: KeycloakService,
    private router: Router,
    private http: HttpClient
  ) {}

  // ================= KEYCLOAK METHODS =================

  isLoggedIn(): boolean {
    return this.keycloakService.isLoggedIn();
  }

  getUserRoles(): string[] {
    return this.keycloakService.getUserRoles();
  }

  getAccessToken(): Promise<string> {
    return this.keycloakService.getValidToken();
  }

  isAdmin(): boolean {
    return this.keycloakService.isAdmin();
  }

  isUser(): boolean {
    return this.keycloakService.isUser();
  }

  logout(): void {
    this.clearCurrentUser();
    this.keycloakService.logout();
  }

  login(): void {
    this.keycloakService.login();
  }

  redirectAfterLogin(): void {
    const roles = this.getUserRoles();
    const currentUrl = this.router.url;

    if (currentUrl.startsWith('/admin') && roles.includes('ROLE_ADMIN')) return;
    if (currentUrl.startsWith('/user') && roles.includes('ROLE_USER')) return;

    if (roles.includes('ROLE_ADMIN')) this.router.navigate(['/admin/allgameweek']);
    else if (roles.includes('ROLE_USER')) this.router.navigate(['/user/user-gameweek-list']);
    else this.router.navigate(['/unauthorized']);
  }

  // ================= USER METHODS =================

  getCurrentUserId(): number | null {
    return this.currentUserSubject.value?.id || null;
  }

  getCurrentUserKeycloakId(): string | null {
    try {
      return this.keycloakService.getKeycloakId();
    } catch (error) {
      console.error('Error getting Keycloak ID:', error);
      return null;
    }
  }

  getCurrentUser(): Observable<CurrentUser> {
    const cached = this.currentUserSubject.value;
    return cached ? of(cached) : this.loadCurrentUser();
  }

  getCurrentUserSync(): CurrentUser | null {
    return this.currentUserSubject.value;
  }

  clearCurrentUser(): void {
    this.currentUserSubject.next(null);
  }

  initializeAuth(): Observable<CurrentUser | null> {
    return this.isLoggedIn() ? this.loadCurrentUser() : of(null);
  }

  loadCurrentUser(): Observable<CurrentUser> {
    if (!this.isLoggedIn()) return throwError(() => new Error('User not authenticated'));

    return this.http.get<CurrentUser>(`${this.userApiUrl}/me`).pipe(
      tap(user => this.currentUserSubject.next(user)),
      catchError(error => {
        console.error('Failed to load current user:', error);
        this.currentUserSubject.next(null);
        return throwError(() => error);
      })
    );
  }
getCurrentUserBalance(): Observable<number> {
  return this.http.get<{ balance: number }>(`${this.userApiUrl}/user-balance`).pipe(
    map(res => res.balance)
  );
}

  // ================= REGISTRATION =================

registerUser(userData: any): Observable<any> {
  return this.http.post(`${this.registrationApiUrl}/register`, userData).pipe(
    tap(response => {
      console.log('✅ Registration successful:', response);
    }),
    catchError(error => {
      console.error('❌ Registration failed:', error);
      
      // Handle specific error types
      if (error.status === 409) {
        throw new Error('Un compte avec ce nom d\'utilisateur ou email existe déjà.');
      } else if (error.status === 400) {
        throw new Error('Données d\'inscription invalides. Veuillez vérifier vos informations.');
      } else if (error.error && error.error.message) {
        throw new Error(error.error.message);
      } else {
        throw new Error('Erreur lors de l\'inscription. Veuillez réessayer.');
      }
    })
  );
}



  // ================= AUTO-CREATE AFTER LOGIN =================

  autoCreateAppUser(): Observable<CurrentUser> {
    return from(this.getAccessToken()).pipe(
      switchMap(token =>
        this.http.post<CurrentUser>(
          `${this.userApiUrl}/auto-create`,
          {},
          { headers: { Authorization: `Bearer ${token}` } }
        )
      ),
      tap(user => this.currentUserSubject.next(user)),
      catchError(error => {
        console.error('Auto-create app user failed:', error);
        return throwError(() => error);
      })
    );
  }

  // ================= UTILITIES =================

  getCurrentUsername(): string | null {
    return this.keycloakService.getUsername();
  }

  getCurrentUserEmail(): string | null {
    return this.keycloakService.getEmail();
  }

  loginAndLoadUser(): void {
    this.keycloakService.login().then(() => {
      this.loadCurrentUser().subscribe({
        next: user => {
          console.log('User loaded after login:', user);
          this.redirectAfterLogin();
        },
        error: error => {
          console.error('Failed to load user after login:', error);
          this.redirectAfterLogin();
        }
      });
    }).catch(error => console.error('Login failed:', error));
  }
}

