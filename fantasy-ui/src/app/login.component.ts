import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../auth.service';
import { ApiService } from './services/api.service';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  standalone: true,
  imports: [CommonModule],
  template: `
    <div style="text-align:center; margin-top: 2rem;">
      <button *ngIf="!auth.isLoggedIn()" (click)="login()">Login</button>
      <button *ngIf="auth.isLoggedIn()" (click)="logout()">Logout</button>

      <div *ngIf="auth.isLoggedIn()">
        <p>Welcome, {{ auth.identityClaims?.preferred_username || 'User' }}!</p>
        <button (click)="callApi()">Call Secured API</button>
      </div>

      <p *ngIf="message">{{ message }}</p>
    </div>
  `
})
export class LoginComponent {
  message: string | null = null;

  constructor(public auth: AuthService, private api: ApiService) {}

  login() {
    this.auth.login();
  }

  logout() {
    this.auth.logout();
  }

  callApi() {
    this.api.getUserData().subscribe({
      next: (res: any) => (this.message = res),
      error: (err: HttpErrorResponse) => (this.message = 'Error: ' + err.message)
    });
  }
}
