// src/app/keycloak.service.ts - FIXED VERSION
import { Injectable } from '@angular/core';
import Keycloak from 'keycloak-js';

@Injectable({
  providedIn: 'root'
})
export class KeycloakService {
  private keycloak: Keycloak;
  private isInitialized = false;
  private initPromise: Promise<boolean> | null = null;
  private cachedRoles: string[] | null = null;
  private lastLoginCheck: number = 0;
  private readonly LOGIN_CHECK_INTERVAL = 5000; // 5 secondes
  private cachedLoginStatus: boolean | null = null;

  constructor() {
    this.keycloak = new Keycloak({
      url: 'http://localhost:8180',
      realm: 'football-fantasy',
      clientId: 'angular-client'
    });

    this.init();
  }

  async init(): Promise<boolean> {
    if (this.initPromise) {
      return this.initPromise;
    }

    this.initPromise = (async () => {
      try {
        // Skip SSO check if we're on the registration page
        if (window.location.pathname === '/register') {
          this.isInitialized = true;
          return false;
        }

        const authenticated = await this.keycloak.init({ 
          onLoad: 'check-sso', 
          checkLoginIframe: false,
          pkceMethod: 'S256',
          enableLogging: true,
        });
        this.isInitialized = true;

        return authenticated;
      } catch (error) {
        this.isInitialized = false;
        this.initPromise = null;
        return false;
      }
    })();

    return this.initPromise;
  }

  async getValidToken(): Promise<string> {
    if (!this.isInitialized) {
    }
    
    try {

      
      const updated = await this.keycloak.updateToken(30);
      
      const token = this.keycloak.token || '';

      
      return token;
    } catch (error) {
      this.keycloak.login();
      throw error;
    }
  }


  async login(redirectUri?: string): Promise<void> {
    await this.keycloak.login({
      redirectUri: redirectUri ? window.location.origin + redirectUri : window.location.origin
    });
  }

  getUsername(): string {
    return this.keycloak.tokenParsed?.['preferred_username'] || '';
  }

  isLoggedIn(): boolean {
    const now = Date.now();
    
    // Si on est sur la page d'inscription, ne pas vérifier le statut
    if (window.location.pathname === '/register') {
      return false;
    }

    // Utiliser le cache si disponible et pas expiré
    if (this.cachedLoginStatus !== null && now - this.lastLoginCheck < this.LOGIN_CHECK_INTERVAL) {
      return this.cachedLoginStatus;
    }

    this.lastLoginCheck = now;
    this.cachedLoginStatus = this.keycloak.authenticated || false;
    
    // Ne logger que si le statut a changé
    if (this.cachedLoginStatus !== this.keycloak.authenticated) {

    }
    
    return this.cachedLoginStatus;
  }

  // FIX: Simplified role checking using tokenParsed directly
  getUserRoles(): string[] {
    
    if (!this.keycloak.tokenParsed) {
      return [];
    }

    const realmAccess = this.keycloak.tokenParsed['realm_access'];
    
    if (!realmAccess || !realmAccess['roles']) {
      return [];
    }

    const roles = realmAccess['roles'];
    
    // Format roles to match backend expectations
    const formattedRoles = roles.map((role: string) => {
      const roleName = role.toUpperCase();
      return roleName.startsWith('ROLE_') ? roleName : `ROLE_${roleName}`;
    });
    
    return formattedRoles;
  }

  hasRole(role: string): boolean {
    
    const roles = this.getUserRoles();
    
    // Check role with and without ROLE_ prefix
    const roleToCheck = role.toUpperCase();
    const hasRole = roles.some((r: string) => 
      r === roleToCheck || 
      r === `ROLE_${roleToCheck}` || 
      r === roleToCheck.replace('ROLE_', '')
    );
    
    return hasRole;
  }

  isAdmin(): boolean {
    return this.hasRole('admin');
  }

  isUser(): boolean {
    return this.hasRole('user');
  }
  logout(): void {
    this.keycloak.logout({
      redirectUri: window.location.origin // Redirect to home page after logout
    });
    this.clearRoleCache(); // Clear any cached data
  }
  private clearRoleCache(): void {
    // Clear any cached user data
    this.cachedRoles = null;
  }
  register(): void {
    window.location.href = '/register';
  }
  private startTokenRefreshLoop(): void {
  setInterval(async () => {
    try {
      const refreshed = await this.keycloak.updateToken(30); // rafraîchir si < 30s
      if (refreshed) {
      }
    } catch (error) {
      this.logout();
    }
  }, 60000); // toutes les 60 secondes
  }
}
