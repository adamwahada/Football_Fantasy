import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app.component';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { KeycloakService } from './app/keycloak.service';
import { AuthInterceptor } from './app/core/interceptors/auth.interceptor';
import { routes } from './app/app.routes'; 
import { provideRouter } from '@angular/router';

const keycloakService = new KeycloakService();

keycloakService.init().then(() => {
  bootstrapApplication(AppComponent, {
    providers: [
      provideHttpClient(withInterceptors([AuthInterceptor])),
      provideRouter(routes), 
      { provide: KeycloakService, useValue: keycloakService }
    ]
  }).catch(err => console.error('Error starting application:', err));
}).catch(err => {
  console.error('Keycloak initialization failed:', err);
});