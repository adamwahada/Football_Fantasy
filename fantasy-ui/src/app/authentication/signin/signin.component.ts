import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { KeycloakService } from '../../keycloak.service';

@Component({
  selector: 'app-signin',
  templateUrl: './signin.component.html'
})
export class SigninComponent implements OnInit {
  private loginAttempted = false;

  constructor(
    private keycloakService: KeycloakService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  async ngOnInit() {
    console.log('🔍 SigninComponent - Vérification état utilisateur');

    if (this.keycloakService.isLoggedIn()) {
      const userRoles = this.keycloakService.getUserRoles();
      console.log('✅ Déjà connecté - rôles:', userRoles);

      // Redirection selon les rôles
    if (userRoles.includes('ROLE_ADMIN')) {
      this.router.navigate(['/admin/referral']);
    } else if (userRoles.includes('ROLE_USER')) {
      this.router.navigate(['/user-dashboard']);
      } else {
        this.router.navigate(['/unauthorized']);
      }
      return;
    }

    if (this.loginAttempted) {
      console.log('⚠️ Connexion déjà tentée');
      return;
    }

    this.loginAttempted = true;

    try {
      console.log('🔄 Démarrage de la connexion Keycloak...');
      await this.keycloakService.login(); // Le redirectUri est déjà géré dans le service
    } catch (error) {
      console.error('❌ Échec de connexion:', error);
      this.loginAttempted = false;
    }
  }
  login(): void {
    // Ajoute ici la logique de connexion avec Keycloak
    console.log('Login avec Keycloak');
  }
}
