import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { KeycloakService } from '../../keycloak.service';
import { from, throwError } from 'rxjs';
import { switchMap, catchError } from 'rxjs/operators';

export const AuthInterceptor: HttpInterceptorFn = (req: HttpRequest<unknown>, next: HttpHandlerFn) => {
  const keycloakService = inject(KeycloakService);
  

  
  // Skip authentication for non-API requests and registration endpoint
  if (!req.url.includes('/api/') || req.url.includes('/api/user/register')) {
    return next(req);
  }
  
  // If request already has Authorization header, pass it through
  if (req.headers.has('Authorization')) {
    return next(req);
  }

  // Get token and add to request
  return from(keycloakService.getValidToken()).pipe(
    switchMap(token => {
      if (!token) {
        keycloakService.login();
        return throwError(() => new Error('No token available'));
      }

      // Clone request with token
      const authReq = req.clone({
        headers: req.headers.set('Authorization', `Bearer ${token}`)
      });



      return next(authReq).pipe(
        catchError((error: HttpErrorResponse) => {

          
          if (error.status === 401 || error.status === 403) {
            return from(keycloakService.getValidToken()).pipe(
              switchMap(newToken => {
                if (newToken && newToken !== token) {
                  const retryReq = req.clone({
                    headers: req.headers.set('Authorization', `Bearer ${newToken}`)
                  });
                  return next(retryReq);
                } else {
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
      // Only redirect to login on 401/403
      if (error.status === 401 || error.status === 403) {
        keycloakService.login();
      }
      // For all other errors, just propagate them
      return throwError(() => error);
    })
  );
};
