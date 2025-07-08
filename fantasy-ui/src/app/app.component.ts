import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApiService } from './services/api.service';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterOutlet } from '@angular/router';
import { AuthService } from './core/services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent implements OnInit {
  message: string = '';
  error: string = '';

  constructor(
    public auth: AuthService,
    private api: ApiService
  ) {}

  ngOnInit() {
    this.auth.isLoggedIn();
  }

  login() {
    this.auth.login();
  }

  logout(): void {
    this.auth.logout();
  }

  register(): void {
    // Optional: implement registration route
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
