// src/app/keycloak.service.ts
import { Injectable } from '@angular/core';
import Keycloak from 'keycloak-js';

@Injectable({
  providedIn: 'root'
})
export class KeycloakService {
  public keycloak: Keycloak;
  private isInitialized = false;
  private initPromise: Promise<boolean> | null = null;
  private cachedRoles: string[] | null = null;
  private lastLoginCheck: number = 0;
  private readonly LOGIN_CHECK_INTERVAL = 5000;
  private cachedLoginStatus: boolean | null = null;

  constructor() {
    this.keycloak = new Keycloak({
      url: 'http://localhost:8180',
      realm: 'football-fantasy',
      clientId: 'angular-client'
    });

    this.init();
    this.startTokenRefreshLoop();
  }

  /**
   * Initialize Keycloak
   */
  async init(): Promise<boolean> {
    if (this.initPromise) return this.initPromise;

    this.initPromise = (async () => {
      try {
        // Always initialize Keycloak, even for "register" flow
        const authenticated = await this.keycloak.init({ 
          onLoad: 'check-sso', 
          checkLoginIframe: false,
          pkceMethod: 'S256',
          enableLogging: true,
        });
        this.isInitialized = true;
        return authenticated;
      } catch (error) {
        console.error('Keycloak init failed:', error);
        this.isInitialized = false;
        this.initPromise = null;
        return false;
      }
    })();

    return this.initPromise;
  }

  /**
   * Login via Keycloak (automatic user creation on backend)
   */
  async login(redirectUri?: string): Promise<void> {
    if (!this.isInitialized) await this.init();

    await this.keycloak.login({
      redirectUri: redirectUri ? window.location.origin + redirectUri : window.location.origin
    });
  }

  /**
   * Logout
   */
  logout(): void {
    this.keycloak.logout({
      redirectUri: window.location.origin
    });
    this.clearRoleCache();
  }

  /**
   * Get a valid token
   */
  async getValidToken(): Promise<string> {
    if (!this.isInitialized) await this.init();
    try {
      await this.keycloak.updateToken(30);
      return this.keycloak.token || '';
    } catch (error) {
      console.error('Token refresh failed, forcing login:', error);
      await this.login();
      throw error;
    }
  }

  // ===== USER INFO METHODS =====

  getKeycloakId(): string | null {
    return this.keycloak.tokenParsed?.sub || null;
  }

  getEmail(): string | null {
    return this.keycloak.tokenParsed?.['email'] || null;
  }

  getUsername(): string {
    return this.keycloak.tokenParsed?.['preferred_username'] || '';
  }

  getFirstName(): string | null {
    return this.keycloak.tokenParsed?.['given_name'] || null;
  }

  getLastName(): string | null {
    return this.keycloak.tokenParsed?.['family_name'] || null;
  }

  getUserProfile(): any {
    return this.keycloak.tokenParsed || null;
  }

  isTokenExpired(): boolean {
    return this.keycloak.isTokenExpired();
  }

  isLoggedIn(): boolean {
    const now = Date.now();
    if (this.cachedLoginStatus !== null && now - this.lastLoginCheck < this.LOGIN_CHECK_INTERVAL) {
      return this.cachedLoginStatus;
    }

    this.lastLoginCheck = now;
    this.cachedLoginStatus = this.keycloak.authenticated || false;
    return this.cachedLoginStatus;
  }

  // ===== ROLE METHODS =====
  getUserRoles(): string[] {
    const roles = this.keycloak.tokenParsed?.['realm_access']?.['roles'] || [];
    return roles.map((r: string) => r.startsWith('ROLE_') ? r : `ROLE_${r.toUpperCase()}`);
  }

  hasRole(role: string): boolean {
    const roleToCheck = role.toUpperCase();
    return this.getUserRoles().some(r =>
      r === roleToCheck || r === `ROLE_${roleToCheck}` || r === roleToCheck.replace('ROLE_', '')
    );
  }

  isAdmin(): boolean { return this.hasRole('admin'); }
  isUser(): boolean { return this.hasRole('user'); }

  // ===== PRIVATE HELPERS =====
  private clearRoleCache(): void { this.cachedRoles = null; }

  private startTokenRefreshLoop(): void {
    setInterval(async () => {
      if (!this.isInitialized) return;
      try {
        await this.keycloak.updateToken(30);
      } catch {
        this.logout();
      }
    }, 60000); // every 60s
  }
}
