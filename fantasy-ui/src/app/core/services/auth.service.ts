// AuthService corrigé - Prioriser l'ID réel de la base de données

import { Injectable } from '@angular/core';
import { KeycloakService } from '../../keycloak.service';
import { Router } from '@angular/router';
import { JwtHelperService } from '@auth0/angular-jwt';
import { HttpClient } from "@angular/common/http";
import { environment } from "../../../environments/environment";

@Injectable({ providedIn: 'root' })
export class AuthService {
  private jwtHelper = new JwtHelperService();
  private cachedUserId: number | null = null;
  private realUserIdCache: number | null = null; // Cache séparé pour l'ID réel

  constructor(
      public keycloakService: KeycloakService,
      private router: Router,
      private http: HttpClient
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
    this.realUserIdCache = null; // Nettoyer aussi le cache de l'ID réel
    localStorage.removeItem('cachedUserId');
    localStorage.removeItem('realUserId'); // Nouveau cache pour l'ID réel
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

  // MÉTHODE PRINCIPALE - Utilise toujours l'ID réel de la base de données
  async getUserId(): Promise<number> {
    return await this.getRealUserId();
  }

  // MÉTHODE POUR RÉCUPÉRER L'ID RÉEL DE LA BASE DE DONNÉES
  async getRealUserId(): Promise<number> {
    try {
      console.log('🔍 AuthService.getRealUserId() called');

      // 1. Vérifier le cache en mémoire
      if (this.realUserIdCache && this.realUserIdCache > 0) {
        console.log('✅ Using cached real user ID:', this.realUserIdCache);
        return this.realUserIdCache;
      }

      // 2. Vérifier localStorage
      const cachedRealId = localStorage.getItem('realUserId');
      if (cachedRealId && Number(cachedRealId) > 0) {
        this.realUserIdCache = Number(cachedRealId);
        console.log('✅ Using cached real user ID from localStorage:', this.realUserIdCache);
        return this.realUserIdCache;
      }

      // 3. Appel API pour récupérer l'ID réel
      const token = await this.getToken();
      if (!token) {
        console.error('❌ No token available');
        return 0;
      }

      const decoded = this.jwtHelper.decodeToken(token);
      const keycloakUuid = decoded.sub;

      if (!keycloakUuid) {
        console.error('❌ No UUID found in token');
        return 0;
      }

      console.log('🔄 Calling API to get real user ID for UUID:', keycloakUuid);

      const response = await this.http.get<number>(
          `${environment.apiUrl}/api/chat/user-id/${keycloakUuid}`
      ).toPromise();

      if (response && response > 0) {
        this.realUserIdCache = response;
        localStorage.setItem('realUserId', response.toString());
        console.log('✅ Got real user ID from API:', this.realUserIdCache);
        return this.realUserIdCache;
      }

      console.error('❌ Invalid response from API:', response);
      return 0;

    } catch (error) {
      console.error('❌ Error in getRealUserId:', error);

      // Fallback vers l'ID temporaire si l'API ne fonctionne pas
      const tempId = localStorage.getItem('tempUserId');
      if (tempId && Number(tempId) > 0) {
        console.log('⚠️ Using temp user ID as fallback:', Number(tempId));
        return Number(tempId);
      }

      return 0;
    }
  }

  // MÉTHODE DE FALLBACK - Génère un ID basé sur l'UUID (à utiliser seulement si l'API ne fonctionne pas)
  async getFallbackUserId(): Promise<number> {
    try {
      console.log('🔄 Using fallback method for user ID');

      // Vérifier le cache
      if (this.cachedUserId && this.cachedUserId > 0) {
        return this.cachedUserId;
      }

      // Récupérer depuis le token
      const token = await this.getToken();
      if (!token) return 0;

      const decoded = this.jwtHelper.decodeToken(token);

      // Essayer les propriétés numériques du token en premier
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
          return this.cachedUserId;
        }
      }

      // Créer un ID basé sur l'UUID en dernier recours
      const uuid = decoded.sub || decoded.preferred_username;
      if (uuid) {
        const fallbackId = this.createConsistentId(uuid);
        this.cachedUserId = fallbackId;
        localStorage.setItem('cachedUserId', fallbackId.toString());
        return fallbackId;
      }

      return 0;
    } catch (error) {
      console.error('Error in getFallbackUserId:', error);
      return 0;
    }
  }

  // Créer un ID numérique consistant basé sur une chaîne
  private createConsistentId(str: string): number {
    let hash = 0;
    for (let i = 0; i < str.length; i++) {
      const char = str.charCodeAt(i);
      hash = ((hash << 5) - hash) + char;
      hash = hash & hash;
    }
    return Math.abs(hash) % 999999 + 1;
  }

  // Version synchrone (utilise le cache)
  async getUserIdSync(): Promise<number> {
    // Prioriser le cache de l'ID réel
    if (this.realUserIdCache && this.realUserIdCache > 0) {
      return this.realUserIdCache;
    }

    // Vérifier localStorage pour l'ID réel
    const cachedRealId = localStorage.getItem('realUserId');
    if (cachedRealId && Number(cachedRealId) > 0) {
      this.realUserIdCache = Number(cachedRealId);
      return this.realUserIdCache;
    }

    // Sinon, essayer de récupérer l'ID réel
    return await this.getRealUserId();
  }

  // Méthode pour forcer le refresh de l'ID utilisateur
  async refreshUserId(): Promise<number> {
    console.log('🔄 Forcing user ID refresh...');
    this.cachedUserId = null;
    this.realUserIdCache = null;
    localStorage.removeItem('cachedUserId');
    localStorage.removeItem('realUserId');
    return await this.getRealUserId();
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
    console.log('Real User ID Cache:', this.realUserIdCache);
    console.log('Fallback User ID Cache:', this.cachedUserId);
    console.log('LocalStorage realUserId:', localStorage.getItem('realUserId'));
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

    const realUserId = await this.getRealUserId();
    console.log('Real User ID from API:', realUserId);

    const fallbackUserId = await this.getFallbackUserId();
    console.log('Fallback User ID:', fallbackUserId);

    console.log('🐛 === END AUTH DEBUG ===');
  }

  // Méthode pour définir manuellement un ID utilisateur (pour les tests)
  setTempUserId(userId: number): void {
    this.realUserIdCache = userId;
    this.cachedUserId = userId;
    localStorage.setItem('tempUserId', userId.toString());
    localStorage.setItem('realUserId', userId.toString());
    console.log('🆘 Temporary user ID set to:', userId);
  }
}