import { Component } from '@angular/core';
import { RouterModule, Router, Event, NavigationStart, NavigationEnd } from '@angular/router';
import { AuthService } from './core/services/auth.service'; // adapte le chemin selon ton arborescence
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {
  currentUrl!: string;

  constructor(
    public _router: Router,
    public auth: AuthService
  ) {
    this._router.events.subscribe((routerEvent: Event) => {
      if (routerEvent instanceof NavigationStart) {
        this.currentUrl = routerEvent.url.substring(
          routerEvent.url.lastIndexOf('/') + 1
        );
      }
      if (routerEvent instanceof NavigationEnd) {
        // navigation terminée
      }
      window.scrollTo(0, 0); // scroll en haut après chaque navigation
    });
  }

  login() {
    this.auth.login();
  }

  logout(): void {
    this.auth.logout();
  }

  register(): void {
    // Implémente l'inscription ici
  }
}
