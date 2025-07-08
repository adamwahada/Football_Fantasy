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
    const userRoles = authService.getUserRoles().map(role => role.toUpperCase());
    const hasRequiredRole = requiredRoles.some(role => 
      userRoles.includes(role.toUpperCase()) || 
      userRoles.includes(`ROLE_${role.toUpperCase()}`) ||
      userRoles.includes(role.toUpperCase().replace('ROLE_', ''))
    );

    if (!hasRequiredRole) {
      console.warn('❌ Accès refusé - Rôle insuffisant', {
        required: requiredRoles,
        found: userRoles,
      });

      router.navigate(['/unauthorized']); 
      return false;
    }
  }

  return true;
};
