import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { KeycloakService } from './keycloak.service';
import { ApiService } from './services/api.service';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterOutlet } from '@angular/router';
import { Router } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {
  userRoles: string[] = [];
  message: string = '';
  error: string = '';

  constructor(
    public keycloak: KeycloakService,
    private api: ApiService,
    private router: Router
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
    this.router.navigate(['/register']);
  }

}
