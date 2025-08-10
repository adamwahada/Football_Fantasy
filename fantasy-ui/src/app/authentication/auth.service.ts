import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
import { Router } from '@angular/router';

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phone?: string;
  country?: string;
  address?: string;
  postalNumber?: string;
  birthDate?: string;
  referralCode?: string;
  termsAccepted: boolean;
  recaptchaToken: string;
}

declare global {
  interface Window {
    grecaptcha: {
      render: (element: string | HTMLElement, options: any) => number;
      getResponse: (widgetId: number) => string;
      reset: (widgetId: number) => void;
      ready: (callback: () => void) => void;
      execute: (widgetId: number, options?: any) => Promise<string>;
    };
    onRecaptchaLoad: () => void;
  }
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:9090/fantasy/api/user';
  private readonly RECAPTCHA_SITE_KEY = '6LfgHGUrAAAAAIYJZpivfvWwdel4PdGulFnPSXSF';
  private recaptchaWidgetId: number | null = null;

  constructor(
    private http: HttpClient,
    private router: Router
  ) {
    this.loadRecaptchaScript();
  }

  private loadRecaptchaScript(): void {
    if (!document.getElementById('recaptcha-script')) {
      const script = document.createElement('script');
      script.id = 'recaptcha-script';
      script.src = 'https://www.google.com/recaptcha/api.js?render=explicit&hl=fr';
      script.async = true;
      script.defer = true;
      document.head.appendChild(script);
    }
  }

  /**
   * Enregistre un utilisateur dans Keycloak via le backend.
   */
  registerUser(data: RegisterRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/register`, data).pipe(
      tap(() => {
        console.log('✅ Inscription réussie');
        this.router.navigate(['/signin']);
      }),
      catchError((error: HttpErrorResponse) => {
        console.error('❌ Erreur d\'inscription', error);
        let errorMessage = 'Une erreur est survenue pendant l\'inscription.';

        // Gestion des erreurs spécifiques
        if (error.status === 409 && typeof error.error === 'string') {
          // Erreur 409 personnalisée (ex: utilisateur déjà existant)
          errorMessage = 'Ce nom d\'utilisateur ou cette adresse email est déjà associé à un compte. Veuillez en choisir un autre ou vous connecter.';
        } else if (error.status === 400 && typeof error.error === 'string') {
          errorMessage = error.error;
        } else if (error.error instanceof ErrorEvent) {
          errorMessage = error.error.message;
        } else if (typeof error.error === 'string') {
          errorMessage = error.error;
        }

        return throwError(() => new Error(errorMessage));
      })
    );
  }

  /**
   * Initialise le reCAPTCHA dans un conteneur HTML donné
   */
  initializeRecaptcha(containerId: string): Promise<void> {
    return new Promise((resolve, reject) => {
      if (!window.grecaptcha) {
        return reject(new Error('reCAPTCHA non chargé'));
      }

      const container = document.getElementById(containerId);
      if (!container) {
        return reject(new Error('Conteneur reCAPTCHA introuvable'));
      }

      try {
        container.innerHTML = '';
        this.recaptchaWidgetId = window.grecaptcha.render(container, {
          sitekey: this.RECAPTCHA_SITE_KEY,
          theme: 'light',
          size: 'normal',
          callback: () => resolve(),
          'expired-callback': () => {
            if (this.recaptchaWidgetId !== null) {
              window.grecaptcha.reset(this.recaptchaWidgetId);
            }
          },
          'error-callback': () => reject(new Error('Erreur reCAPTCHA'))
        });
      } catch (error) {
        reject(error);
      }
    });
  }

  /**
   * Vérifie si l'utilisateur est connecté ET si le token est valide
   */
  isLoggedIn(): boolean {
    const token = localStorage.getItem('token');
    
    if (!token) {
      return false;
    }

    try {
      // Vérifier si le token est expiré
      const payload = JSON.parse(atob(token.split('.')[1]));
      const currentTime = Math.floor(Date.now() / 1000);
      
      if (payload.exp && payload.exp < currentTime) {
        console.log('🔒 Token expiré, déconnexion automatique');
        this.logout();
        return false;
      }
      
      return true;
    } catch (error) {
      console.error('❌ Token invalide', error);
      this.logout();
      return false;
    }
  }

  /**
   * Récupère les rôles de l'utilisateur depuis le token
   */
// Ajoutez cette méthode dans votre AuthService pour déboguer
debugToken(): void {
  const token = localStorage.getItem('token');
  if (!token) {
    console.log('❌ Aucun token trouvé');
    return;
  }

  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    console.log('🔍 Contenu complet du token:', payload);
    
    // Vérifier tous les emplacements possibles des rôles
    console.log('🔍 Rôles trouvés:', {
      roles: payload.roles,
      realm_access: payload.realm_access,
      resource_access: payload.resource_access,
      scope: payload.scope,
      authorities: payload.authorities
    });
  } catch (error) {
    console.error('❌ Erreur lors du décodage du token:', error);
  }
}

// Méthode getUserRoles() améliorée
getUserRoles(): string[] {
  const token = localStorage.getItem('token');
  if (!token) return [];
  
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    
    // Debug: afficher le payload complet
    console.log('🔍 Payload token:', payload);
    
    // Vérifier tous les emplacements possibles pour les rôles Keycloak
    let roles: string[] = [];
    
    // 1. Rôles directs
    if (payload.roles) {
      roles = [...roles, ...payload.roles];
    }
    
    // 2. Realm access roles (format Keycloak standard)
    if (payload.realm_access?.roles) {
      roles = [...roles, ...payload.realm_access.roles];
    }
    
    // 3. Resource access roles (pour des clients spécifiques)
    if (payload.resource_access) {
      Object.keys(payload.resource_access).forEach(client => {
        if (payload.resource_access[client]?.roles) {
          roles = [...roles, ...payload.resource_access[client].roles];
        }
      });
    }
    
    // 4. Authorities (format Spring Security)
    if (payload.authorities) {
      roles = [...roles, ...payload.authorities];
    }
    
    // 5. Scope (parfois les rôles sont dans le scope)
    if (payload.scope) {
      const scopeRoles = payload.scope.split(' ').filter((s: string) => s.startsWith('role_'));
      roles = [...roles, ...scopeRoles.map((r: string) => r.replace('role_', ''))];
    }
    
    // Supprimer les doublons
    roles = [...new Set(roles)];
    
    console.log('🔍 Rôles extraits:', roles);
    return roles;
  } catch (error) {
    console.error('❌ Erreur lors de la récupération des rôles:', error);
    return [];
  }
}

  /**
   * Déconnecte l'utilisateur
   */
  logout(): void {
    localStorage.removeItem('token');
    // Ne pas rediriger automatiquement pour éviter les boucles
    // this.router.navigate(['/signin']);
  }

  /**
   * Connecte l'utilisateur
   */
  login(credentials: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/login`, credentials).pipe(
      tap((response: any) => {
        if (response.token) {
          localStorage.setItem('token', response.token);
          console.log('✅ Connexion réussie');
          
          // 👉 Forcer un petit délai pour laisser le token être bien écrit et lu
          setTimeout(() => {
            const roles = this.getUserRoles();
            console.log('✅ Rôles après login :', roles);
          
            if (roles.includes('admin')) {
              this.router.navigate(['admin/allgameweek']);
            } else {
              this.router.navigate(['user/user-gameweek-list']);
            }
          }, 100);
        } else {
          throw new Error('Token manquant dans la réponse');
        }
      }),
      catchError((error: HttpErrorResponse) => {
        console.error('❌ Erreur de connexion', error);
        let errorMessage = 'Identifiants incorrects';
        
        if (error.status === 401) {
          errorMessage = 'Nom d\'utilisateur ou mot de passe incorrect';
        } else if (error.status === 0) {
          errorMessage = 'Impossible de se connecter au serveur';
        }
        
        return throwError(() => new Error(errorMessage));
      })
    );
  }
  checkEmailVerified(): Observable<boolean> {
    return this.http.get<boolean>(`${this.apiUrl}/email-verified`).pipe(
      catchError((error: HttpErrorResponse) => {
        console.error('❌ Erreur lors de la vérification de l\'email', error);
        return throwError(() => new Error('Erreur lors de la vérification de l\'email'));
      })
    );
  }
}