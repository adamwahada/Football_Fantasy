import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { KeycloakService } from './keycloak.service';
import { from, throwError } from 'rxjs';
import { switchMap, catchError } from 'rxjs/operators';

export const AuthInterceptor: HttpInterceptorFn = (req: HttpRequest<unknown>, next: HttpHandlerFn) => {
  const keycloakService = inject(KeycloakService);
  
  console.log('=== Intercepting request ===');
  console.log('URL:', req.url);
  console.log('Method:', req.method);
  
  // Skip authentication for non-API requests
  if (!req.url.includes('/api/')) {
    console.log('Skipping non-API request');
    return next(req);
  }
  
  // If request already has Authorization header, pass it through
  if (req.headers.has('Authorization')) {
    console.log('Request already has Authorization header');
    return next(req);
  }

  // Get token and add to request
  return from(keycloakService.getValidToken()).pipe(
    switchMap(token => {
      if (!token) {
        console.log('No token available, redirecting to login...');
        keycloakService.login();
        return throwError(() => new Error('No token available'));
      }

      // Clone request with token
      const authReq = req.clone({
        headers: req.headers.set('Authorization', `Bearer ${token}`)
      });

      console.log('=== Modified request ===');
      console.log('Authorization header added:', !!authReq.headers.get('Authorization'));
      console.log('Token length:', token.length);

      return next(authReq).pipe(
        catchError((error: HttpErrorResponse) => {
          console.error('=== Request failed ===');
          console.error('Error status:', error.status);
          console.error('Error message:', error.message);
          console.error('Error url:', error.url);
          
          if (error.status === 401 || error.status === 403) {
            console.log('Authentication/Authorization failed, attempting token refresh...');
            return from(keycloakService.getValidToken()).pipe(
              switchMap(newToken => {
                if (newToken && newToken !== token) {
                  console.log('New token obtained, retrying request');
                  const retryReq = req.clone({
                    headers: req.headers.set('Authorization', `Bearer ${newToken}`)
                  });
                  return next(retryReq);
                } else {
                  console.log('Token refresh failed, redirecting to login');
                  keycloakService.login();
                  return throwError(() => error);
                }
              }),
              catchError(() => {
                keycloakService.login();
                return throwError(() => error);
              })
            );
          }
          
          return throwError(() => error);
        })
      );
    }),
    catchError(error => {
      console.error('Failed to get token:', error);
      keycloakService.login();
      return throwError(() => error);
    })
  );
};
