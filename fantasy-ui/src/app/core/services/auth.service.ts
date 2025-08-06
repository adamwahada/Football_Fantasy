import { Injectable } from '@angular/core';
import { KeycloakService } from '../../keycloak.service';
import { Router } from '@angular/router';
import { JwtHelperService } from '@auth0/angular-jwt';


@Injectable({ providedIn: 'root' })
export class AuthService {
  private jwtHelper = new JwtHelperService();

  constructor(
    public keycloakService: KeycloakService,
    private router: Router
  ) {}

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
    this.keycloakService.logout();
  }

  login(): void {
    this.keycloakService.login();
  }
  
  redirectAfterLogin(): void {
    const roles = this.getUserRoles();
    const currentUrl = this.router.url;
  
    // If we're already on an admin route and user is admin, don't redirect
    if (currentUrl.startsWith('/admin') && roles.includes('ROLE_ADMIN')) {
      return;
    }
  
    // If we're already on a user route and user has user role, don't redirect
    if (currentUrl.startsWith('/user') && roles.includes('ROLE_USER')) {
      return;
    }
  
    // Otherwise, perform default redirects
    if (roles.includes('ROLE_ADMIN')) {
      this.router.navigate(['/admin/referral']);
      } else if (roles.includes('ROLE_USER')) {
      this.router.navigate(['/user-dashboard']);
    } else {
      this.router.navigate(['/unauthorized']);
    }
  }
  getToken(): string {
    return localStorage.getItem('access_token') || '';
  }

  getUserId(): number {
    const token = this.getToken();
    if (!token) return 0;
    const decoded = this.jwtHelper.decodeToken(token);
    return decoded.sub || 0;
  }

  isAuthenticated(): boolean {
    const token = this.getToken();
    return !this.jwtHelper.isTokenExpired(token);
  }

}
