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
        let errorMessage = 'Une erreur est survenue pendant l’inscription.';

        // Gestion des erreurs spécifiques
        if (error.status === 409 && typeof error.error === 'string') {
          // Erreur 409 personnalisée (ex: utilisateur déjà existant)
          errorMessage = 'Ce nom d’utilisateur ou cette adresse email est déjà associé à un compte. Veuillez en choisir un autre ou vous connecter.';
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
}
