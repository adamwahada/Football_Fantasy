import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { KeycloakService } from './keycloak.service';
import { ApiService } from './services/api.service';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div style="text-align:center; margin-top: 2rem;">
      <h1>Football Fantasy App</h1>

      <div *ngIf="keycloak.isLoggedIn()">
        <p>Welcome, {{ keycloak.getUsername() }}!</p>
        <p>Your roles: {{ userRoles.join(', ') }}</p>

        <div style="margin: 1rem 0;">
          <button (click)="testUserEndpoint()" style="margin: 0.5rem;">Test User Endpoint</button>
          <button (click)="testAdminEndpoint()" style="margin: 0.5rem;" [disabled]="!keycloak.isAdmin()">
            Test Admin Endpoint
          </button>
        </div>

        <div style="margin-top: 1rem;">
          <p><strong>Is Admin:</strong> {{ keycloak.isAdmin() }}</p>
          <p><strong>Is User:</strong> {{ keycloak.isUser() }}</p>
        </div>
      </div>

      <div *ngIf="message" style="margin-top: 1rem; padding: 1rem; border: 1px solid #ccc;">
        <h3>API Response:</h3>
        <pre>{{ message }}</pre>
      </div>

      <div *ngIf="error" style="margin-top: 1rem; padding: 1rem; border: 1px solid red; color: red;">
        <h3>Error:</h3>
        <pre>{{ error }}</pre>
      </div>
    </div>
  `
})
export class DashboardComponent implements OnInit {
  userRoles: string[] = [];
  message = '';
  error = '';

  constructor(
    public keycloak: KeycloakService,
    private api: ApiService
  ) {}

  ngOnInit(): void {
    if (this.keycloak.isLoggedIn()) {
      this.userRoles = this.keycloak.getUserRoles();
    }
  }

  testUserEndpoint() {
    this.clearMessages();
    this.api.getUserData().subscribe({
      next: (response) => {
        this.message = JSON.stringify(response, null, 2);
      },
      error: (error: HttpErrorResponse) => {
        this.error = `Status: ${error.status}, Message: ${error.message}`;
      }
    });
  }

  testAdminEndpoint() {
    this.clearMessages();
    this.api.getAdminData().subscribe({
      next: (response) => {
        this.message = JSON.stringify(response, null, 2);
      },
      error: (error: HttpErrorResponse) => {
        this.error = `Status: ${error.status}, Message: ${error.message}`;
      }
    });
  }

  private clearMessages() {
    this.message = '';
    this.error = '';
  }
}