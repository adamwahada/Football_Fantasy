import { Component } from '@angular/core';
import { CommonModule } from '@angular/common'; // Pour *ngIf
import { AuthService } from '../core/services/auth.service';
import { ApiService } from '../services/api.service';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-test',
  standalone: true,
  imports: [CommonModule], 
  templateUrl: './test.component.html',
  styleUrls: ['./test.component.scss'],
})
export class TestComponent {
  message: string = '';
  error: string = '';

  constructor(
    public auth: AuthService,
    private api: ApiService
  ) {}

  ngOnInit(): void {
    this.auth.isLoggedIn(); // Optionnel : ici tu peux appeler un refresh ou une vérification de session
  }

  testUserEndpoint(): void {
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

  testAdminEndpoint(): void {
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

  private clearMessages(): void {
    this.message = '';
    this.error = '';
  }
}
