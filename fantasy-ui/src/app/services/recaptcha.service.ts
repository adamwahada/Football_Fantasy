import { Injectable } from '@angular/core';

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
export class RecaptchaService {
  private readonly RECAPTCHA_SITE_KEY = '6LfgHGUrAAAAAIYJZpivfvWwdel4PdGulFnPSXSF';
  private recaptchaWidgetId: number | null = null;

  constructor() {
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

  getToken(): string | null {
    if (this.recaptchaWidgetId === null) {
      return null;
    }
    return window.grecaptcha.getResponse(this.recaptchaWidgetId);
  }

  reset(): void {
    if (this.recaptchaWidgetId !== null) {
      window.grecaptcha.reset(this.recaptchaWidgetId);
    }
  }
} 