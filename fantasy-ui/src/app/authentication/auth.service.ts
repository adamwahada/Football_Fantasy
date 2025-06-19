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

  registerUser(data: RegisterRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/register`, data).pipe(
      tap(() => {
        console.log('✅ Registration successful');
        this.router.navigate(['/signin']);
      }),
      catchError((error: HttpErrorResponse) => {
        console.error('❌ Registration error', error);
        let errorMessage = 'An error occurred during registration';
        
        if (error.error instanceof ErrorEvent) {
          // Client-side error
          errorMessage = error.error.message;
        } else {
          // Server-side error
          errorMessage = error.error?.message || error.message;
        }
        
        return throwError(() => new Error(errorMessage));
      })
    );
  }

  initializeRecaptcha(containerId: string): Promise<void> {
    return new Promise((resolve, reject) => {
      if (!window.grecaptcha) {
        reject(new Error('reCAPTCHA not loaded'));
        return;
      }

      const container = document.getElementById(containerId);
      if (!container) {
        reject(new Error('reCAPTCHA container not found'));
        return;
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
          'error-callback': () => reject(new Error('reCAPTCHA error'))
        });
      } catch (error) {
        reject(error);
      }
    });
  }
}
