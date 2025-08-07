// Version simplifiée de l'AuthService - à utiliser si l'API ne fonctionne pas encore

import { Injectable } from '@angular/core';
import { KeycloakService } from '../../keycloak.service';
import { Router } from '@angular/router';
import { JwtHelperService } from '@auth0/angular-jwt';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private jwtHelper = new JwtHelperService();
  private cachedUserId: number | null = null;

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
    this.cachedUserId = null;
    localStorage.removeItem('cachedUserId');
    localStorage.removeItem('tempUserId');
    this.keycloakService.logout();
  }

  login(): void {
    this.keycloakService.login();
  }

  async getToken(): Promise<string> {
    try {
      return await this.keycloakService.getValidToken();
    } catch (error) {
      console.error('Error getting Keycloak token:', error);
      return '';
    }
  }

  getTokenSync(): string {
    try {
      if (this.keycloakService && (this.keycloakService as any).token) {
        return (this.keycloakService as any).token;
      }
      return localStorage.getItem('access_token') || '';
    } catch (error) {
      console.error('Error getting sync token:', error);
      return '';
    }
  }

  // MÉTHODE SIMPLIFIÉE - Sans appel API
  async getUserId(): Promise<number> {
    try {
      console.log('🔍 AuthService.getUserId() called');

      // 1. Vérifier le cache
      if (this.cachedUserId && this.cachedUserId > 0) {
        console.log('✅ Using cached user ID:', this.cachedUserId);
        return this.cachedUserId;
      }

      // 2. Vérifier localStorage
      const tempId = localStorage.getItem('tempUserId');
      if (tempId && Number(tempId) > 0) {
        this.cachedUserId = Number(tempId);
        console.log('✅ Using temp user ID from localStorage:', this.cachedUserId);
        return this.cachedUserId;
      }

      const cachedId = localStorage.getItem('cachedUserId');
      if (cachedId && Number(cachedId) > 0) {
        this.cachedUserId = Number(cachedId);
        console.log('✅ Using cached user ID from localStorage:', this.cachedUserId);
        return this.cachedUserId;
      }

      // 3. Essayer de récupérer depuis le token Keycloak
      console.log('🔄 Trying to get ID from token...');
      const token = await this.getToken();
      if (!token) {
        console.error('❌ No token available');
        return 0;
      }

      const decoded = this.jwtHelper.decodeToken(token);
      console.log('🔓 Decoded token:', decoded);

      // 4. Essayer différentes propriétés du token
      const possibleIds = [
        decoded.userId,
        decoded.id,
        decoded.user_id,
        decoded.database_id,
        decoded.numeric_id
      ];

      for (const id of possibleIds) {
        if (id && Number(id) > 0) {
          this.cachedUserId = Number(id);
          localStorage.setItem('cachedUserId', this.cachedUserId.toString());
          console.log('✅ Found numeric ID in token:', this.cachedUserId);
          return this.cachedUserId;
        }
      }

      // 5. Si aucun ID numérique trouvé, créer un ID temporaire basé sur l'UUID
      const uuid = decoded.sub || decoded.preferred_username;
      if (uuid) {
        // Créer un hash simple mais consistant de l'UUID
        const simpleId = this.createConsistentId(uuid);
        this.cachedUserId = simpleId;
        localStorage.setItem('cachedUserId', simpleId.toString());
        console.log('🔄 Created consistent ID from UUID:', simpleId, 'from', uuid);
        return simpleId;
      }

      console.error('❌ No usable identifier found in token');
      return 0;

    } catch (error) {
      console.error('❌ Error in getUserId:', error);
      return 0;
    }
  }

  // Créer un ID numérique consistant basé sur une chaîne
  private createConsistentId(str: string): number {
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
      const char = str.charCodeAt(i);
      hash = ((hash << 5) - hash) + char;
      hash = hash & hash; // Convert to 32-bit integer
    }
    // S'assurer que c'est un nombre positif entre 1 et 999999
    return Math.abs(hash) % 999999 + 1;
  }

  getUserIdSync(): number {
    if (this.cachedUserId && this.cachedUserId > 0) {
      return this.cachedUserId;
    }

    const tempId = localStorage.getItem('tempUserId');
    if (tempId && Number(tempId) > 0) {
      this.cachedUserId = Number(tempId);
      return this.cachedUserId;
    }

    const cachedId = localStorage.getItem('cachedUserId');
    if (cachedId && Number(cachedId) > 0) {
      this.cachedUserId = Number(cachedId);
      return this.cachedUserId;
    }

    // Essayer de récupérer sync depuis le token
    try {
      const token = this.getTokenSync();
      if (token) {
        const decoded = this.jwtHelper.decodeToken(token);
        const uuid = decoded.sub || decoded.preferred_username;
        if (uuid) {
          const simpleId = this.createConsistentId(uuid);
          this.cachedUserId = simpleId;
          localStorage.setItem('cachedUserId', simpleId.toString());
          return simpleId;
        }
      }
    } catch (error) {
      console.error('Error in sync token processing:', error);
    }

    return 0;
  }

  // Méthode pour forcer le refresh de l'ID utilisateur
  async refreshUserId(): Promise<number> {
    console.log('🔄 Forcing user ID refresh...');
    this.cachedUserId = null;
    localStorage.removeItem('cachedUserId');
    return await this.getUserId();
  }

  isAuthenticated(): boolean {
    return this.keycloakService.isLoggedIn();
  }

  getCurrentUser() {
    try {
      if (this.keycloakService && (this.keycloakService as any).getKeycloakInstance) {
        const keycloak = (this.keycloakService as any).getKeycloakInstance();
        if (keycloak && keycloak.tokenParsed) {
          return {
            id: this.getUserIdSync(),
            uuid: keycloak.tokenParsed.sub,
            username: keycloak.tokenParsed.preferred_username,
            email: keycloak.tokenParsed.email,
            firstName: keycloak.tokenParsed.given_name,
            lastName: keycloak.tokenParsed.family_name,
            roles: this.getUserRoles()
          };
        }
      }

      const userJson = localStorage.getItem('user') || localStorage.getItem('currentUser');
      return userJson ? JSON.parse(userJson) : null;
    } catch (error) {
      console.error('Error getting current user:', error);
      return null;
    }
  }

  getUsername(): string {
    try {
      const token = this.getTokenSync();
      if (token) {
        const decoded = this.jwtHelper.decodeToken(token);
        return decoded.preferred_username || decoded.sub || '';
      }
      return '';
    } catch (error) {
      console.error('Error getting username:', error);
      return '';
    }
  }

  // Méthode utilitaire pour le debug
  async debugUserInfo(): Promise<void> {
    console.log('🐛 === AUTH SERVICE DEBUG ===');
    console.log('Is logged in:', this.isLoggedIn());
    console.log('Cached User ID:', this.cachedUserId);
    console.log('LocalStorage cachedUserId:', localStorage.getItem('cachedUserId'));
    console.log('LocalStorage tempUserId:', localStorage.getItem('tempUserId'));

    const token = this.getTokenSync();
    console.log('Token available:', !!token);

    if (token) {
      try {
        const decoded = this.jwtHelper.decodeToken(token);
        console.log('Token decoded successfully:', {
          sub: decoded.sub,
          preferred_username: decoded.preferred_username,
          userId: decoded.userId,
          id: decoded.id
        });
      } catch (error) {
        console.error('Token decode error:', error);
      }
    }

    const finalUserId = await this.getUserId();
    console.log('Final User ID:', finalUserId);
    console.log('🐛 === END AUTH DEBUG ===');
  }

  // Méthode pour définir manuellement un ID utilisateur (pour les tests)
  setTempUserId(userId: number): void {
    this.cachedUserId = userId;
    localStorage.setItem('tempUserId', userId.toString());
    console.log('🆘 Temporary user ID set to:', userId);
  }
}