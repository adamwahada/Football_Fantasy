import { inject } from '@angular/core';
import { Router, CanActivateFn, ActivatedRouteSnapshot } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // Vérifie si l'utilisateur est connecté
  if (!authService.isLoggedIn()) {
    console.log('❌ Utilisateur non connecté, redirection vers /signin');
    router.navigate(['/signin']);
    return false;
  }

  // Récupérer les rôles attendus
  const requiredRoles = route.data?.['roles'] as string[] | undefined;

  if (requiredRoles && requiredRoles.length > 0) {
    const userRoles = authService.getUserRoles();
    const hasRequiredRole = requiredRoles.some(role => userRoles.includes(role));

    if (!hasRequiredRole) {
      console.warn('❌ Accès refusé - Rôle insuffisant', {
        required: requiredRoles,
        found: userRoles,
      });

      // ✅ Rediriger vers une page d'erreur ou d'accès refusé
      router.navigate(['/unauthorized']); 
      return false;
    }
  }

  return true;
};
