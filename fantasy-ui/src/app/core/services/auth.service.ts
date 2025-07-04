import { Injectable } from '@angular/core';
import { KeycloakService } from '../../keycloak.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  constructor(private keycloakService: KeycloakService) {}

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
}
