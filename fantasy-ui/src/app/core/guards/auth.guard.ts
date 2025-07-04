import { Injectable, inject } from '@angular/core';
import { CanActivateFn, Router, ActivatedRouteSnapshot } from '@angular/router';
import { AuthService } from '../services/auth.service'; 

export const authGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isLoggedIn()) {
    router.navigate(['/authentication/signin']);
    return false;
  }

  const userRoles = authService.getUserRoles();

  const allowedRoles = route.data['roles'] as string[] | undefined;

  if (allowedRoles && !allowedRoles.some(role => userRoles.includes(role))) {
    // Pas le bon rôle
    router.navigate(['/authentication/signin']);
    return false;
  }

  return true;
};
