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

  constructor() {
    this.keycloak = new Keycloak({
      url: 'http://localhost:8180',
      realm: 'football-fantasy',
      clientId: 'angular-client',
    });
  }

  async init(): Promise<boolean> {
    if (this.initPromise) {
      return this.initPromise;
    }

    this.initPromise = (async () => {
      try {
        const authenticated = await this.keycloak.init({ 
          onLoad: 'login-required',
          checkLoginIframe: false,
          pkceMethod: 'S256',
          enableLogging: true,
          silentCheckSsoRedirectUri: window.location.origin + '/assets/silent-check-sso.html'
        });
        this.isInitialized = true;
        console.log('=== Keycloak Initialization ===');
        console.log('Authenticated:', authenticated);
        console.log('Token:', this.keycloak.token ? 'Present' : 'Missing');
        console.log('Token Parsed:', this.keycloak.tokenParsed);
        console.log('Roles:', await this.getUserRoles()); // FIX: await the promise
        return authenticated;
      } catch (error) {
        console.error('Keycloak initialization failed:', error);
        this.isInitialized = false;
        this.initPromise = null;
        return false;
      }
    })();

    return this.initPromise;
  }

  async getValidToken(): Promise<string> {
    if (!this.isInitialized) {
      console.error('Keycloak not initialized');
      throw new Error('Keycloak not initialized');
    }
    
    try {
      console.log('=== Getting Valid Token ===');
      console.log('Current token:', this.keycloak.token ? 'Present' : 'Missing');
      
      const updated = await this.keycloak.updateToken(30);
      console.log('Token updated:', updated);
      
      const token = this.keycloak.token || '';
      console.log('Token length:', token.length);
      console.log('Token preview:', token.substring(0, 50) + '...');
      
      return token;
    } catch (error) {
      console.error('Token refresh failed:', error);
      this.keycloak.login();
      throw error;
    }
  }


  login(): void {
    console.log('Redirecting to login...');
    this.keycloak.login();
  }

  getUsername(): string {
    return this.keycloak.tokenParsed?.['preferred_username'] || '';
  }

  isLoggedIn(): boolean {
    const isLoggedIn = this.keycloak.authenticated || false;
    console.log('=== Checking Login Status ===');
    console.log('Is logged in:', isLoggedIn);
    return isLoggedIn;
  }

  // FIX: Simplified role checking using tokenParsed directly
  getUserRoles(): string[] {
    console.log('=== Getting User Roles ===');
    
    if (!this.keycloak.tokenParsed) {
      console.log('No token parsed available');
      return [];
    }

    const realmAccess = this.keycloak.tokenParsed['realm_access'];
    console.log('Realm Access:', realmAccess);
    
    if (!realmAccess || !realmAccess['roles']) {
      console.log('No roles found in token');
      return [];
    }

    const roles = realmAccess['roles'];
    console.log('Raw roles from token:', roles);
    
    // Format roles to match backend expectations
    const formattedRoles = roles.map((role: string) => {
      const roleName = role.toUpperCase();
      return roleName.startsWith('ROLE_') ? roleName : `ROLE_${roleName}`;
    });
    
    console.log('Formatted roles:', formattedRoles);
    return formattedRoles;
  }

  hasRole(role: string): boolean {
    console.log('=== Checking Role ===');
    console.log('Role to check:', role);
    
    const roles = this.getUserRoles();
    console.log('Available roles:', roles);
    
    // Check role with and without ROLE_ prefix
    const roleToCheck = role.toUpperCase();
    const hasRole = roles.some((r: string) => 
      r === roleToCheck || 
      r === `ROLE_${roleToCheck}` || 
      r === roleToCheck.replace('ROLE_', '')
    );
    
    console.log('Has role:', hasRole);
    return hasRole;
  }

  isAdmin(): boolean {
    return this.hasRole('admin');
  }

  isUser(): boolean {
    return this.hasRole('user');
  }
  logout(): void {
    console.log('=== Logging out ===');
    this.keycloak.logout({
      redirectUri: window.location.origin // Redirect to home page after logout
    });
    this.clearRoleCache(); // Clear any cached data
  }
  private clearRoleCache(): void {
    // Clear any cached user data
    this.cachedRoles = null;
    console.log('User data cleared');
  }
  register(): void {
    console.log('=== Redirecting to registration ===');
    this.keycloak.register({
      redirectUri: window.location.origin + '/dashboard' 
    });
  }
}
