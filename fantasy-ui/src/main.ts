// main.ts
import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app.component';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { KeycloakService } from './app/keycloak.service';
import { AuthInterceptor } from './app/core/interceptors/auth.interceptor';
import { routes } from './app/app.routes';
import { provideRouter } from '@angular/router';

import { importProvidersFrom } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { CustomTranslateLoader } from './assets/i18n/custom-translate-loader';
const keycloakService = new KeycloakService();

keycloakService.init().then(() => {
  bootstrapApplication(AppComponent, {
    providers: [
      provideHttpClient(withInterceptors([AuthInterceptor])),
      provideRouter(routes),
      { provide: KeycloakService, useValue: keycloakService },
      importProvidersFrom(
        TranslateModule.forRoot({
          loader: {
            provide: TranslateLoader,
            useClass: CustomTranslateLoader,
            deps: [HttpClient]
          },
          defaultLanguage: 'en' // optional, set your default language
        })
      )
    ]
  }).catch(err => console.error('Error starting application:', err));
}).catch(err => {
  console.error('Keycloak initialization failed:', err);
});
