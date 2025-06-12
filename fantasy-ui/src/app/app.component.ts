import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { KeycloakService } from './keycloak.service';
import { ApiService } from './services/api.service';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div style="text-align:center; margin-top: 2rem;">
      <h1>Football Fantasy App</h1>
      <div *ngIf="!keycloak.isLoggedIn()">
  <button (click)="login()">Login</button>
  <button (click)="register()">Register</button>
</div>
      
      
      <div *ngIf="keycloak.isLoggedIn()">
        <p>Welcome, {{ keycloak.getUsername() }}!</p>
        <p>Your roles: {{ userRoles.join(', ') }}</p>
        
        <div style="margin: 1rem 0;">
          <button (click)="logout()" style="margin: 0.5rem;">Logout</button>
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
export class AppComponent implements OnInit {
  userRoles: string[] = [];
  message: string = '';
  error: string = '';

  constructor(
    public keycloak: KeycloakService,
    private api: ApiService
  ) {}

  async ngOnInit() {
    if (this.keycloak.isLoggedIn()) {
      this.userRoles = this.keycloak.getUserRoles();
      console.log('=== User Info ===');
      console.log('Username:', this.keycloak.getUsername());
      console.log('Roles:', this.userRoles);
      console.log('Is Admin:', this.keycloak.isAdmin());
      console.log('Is User:', this.keycloak.isUser());
    }
  }

  login() {
    this.keycloak.login();
  }

  logout(): void {
    this.keycloak.logout();
  }

  testUserEndpoint() {
    this.clearMessages();
    console.log('Testing USER endpoint...');
    
    this.api.getUserData().subscribe({
      next: (response) => {
        console.log('USER endpoint success:', response);
        this.message = JSON.stringify(response, null, 2);
      },
      error: (error: HttpErrorResponse) => {
        console.error('USER endpoint error:', error);
        this.error = `Status: ${error.status}, Message: ${error.message}`;
      }
    });
  }

  testAdminEndpoint() {
    this.clearMessages();
    console.log('Testing ADMIN endpoint...');
    
    this.api.getAdminData().subscribe({
      next: (response) => {
        console.log('ADMIN endpoint success:', response);
        this.message = JSON.stringify(response, null, 2);
      },
      error: (error: HttpErrorResponse) => {
        console.error('ADMIN endpoint error:', error);
        this.error = `Status: ${error.status}, Message: ${error.message}`;
      }
    });
  }

  private clearMessages() {
    this.message = '';
    this.error = '';
  }

  register(): void {
    this.keycloak.register();
  }

}
