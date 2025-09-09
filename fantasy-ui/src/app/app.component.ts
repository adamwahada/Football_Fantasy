import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { KeycloakService } from './keycloak.service';
import { Router, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet],
  template: `
    <header style="display:flex; align-items:center; justify-content:space-between; padding: 0.75rem 1rem; border-bottom: 1px solid #eee;">
      <div style="font-weight:600;">Football Fantasy</div>
      <div>
        <ng-container *ngIf="!keycloak.isLoggedIn(); else loggedIn">
          <button (click)="login()">Login</button>
          <button (click)="register()">Register</button>
        </ng-container>
        <ng-template #loggedIn>
          <span style="margin-right: 1rem;">{{ keycloak.getUsername() }}</span>
          <button (click)="logout()">Logout</button>
        </ng-template>
      </div>
    </header>
    <router-outlet></router-outlet>
  `
})
export class AppComponent implements OnInit {

  constructor(
    public keycloak: KeycloakService,
    private router: Router
  ) {}

  ngOnInit(): void {
    if (this.keycloak.isLoggedIn()) {
      const seen = localStorage.getItem('landingSeen') === '1';
      if (!seen) {
        this.router.navigateByUrl('/welcome');
      }
    }
  }

  login() {
    this.keycloak.login();
  }

  logout(): void {
    this.keycloak.logout();
  }

  register(): void {
    this.keycloak.register();
  }
}
