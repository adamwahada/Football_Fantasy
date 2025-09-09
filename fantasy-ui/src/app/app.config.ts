import { importProvidersFrom } from '@angular/core';
import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptors } from '@angular/common/http';
import { AuthInterceptor } from './core/interceptors/auth.interceptor';
import { HttpClientModule } from '@angular/common/http';
// SupportSimpleModule temporairement désactivé pour test

export const appConfig = {
  providers: [
    importProvidersFrom(HttpClientModule),
    provideHttpClient(
      withInterceptors([AuthInterceptor])
    )
  ],
};
