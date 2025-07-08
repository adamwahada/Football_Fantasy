import { Injectable } from '@angular/core';
import { KeycloakService } from '../../keycloak.service';
import { Router } from '@angular/router';

@Injectable({ providedIn: 'root' })
export class AuthService {
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
}
