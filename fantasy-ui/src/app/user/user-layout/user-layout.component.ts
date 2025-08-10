import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { UserSidebarComponent } from '../user-sidebar/user-sidebar.component';
// Import your auth service
// import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-user-layout',
  standalone: true,
  imports: [RouterOutlet, UserSidebarComponent, CommonModule],
  templateUrl: './user-layout.component.html', // Use external template
  styleUrls: ['./user-layout.component.scss']
})
export class UserLayoutComponent {
  sidebarCollapsed = false;
  
  // Add your auth service here
  // constructor(public auth: AuthService) {}
  auth: any = null; // Replace with actual auth service injection
  
  toggleSidebar() {
    this.sidebarCollapsed = !this.sidebarCollapsed;
  }
  
  // Mock auth methods for now - replace with actual auth service
  isLoggedIn(): boolean {
    return this.auth?.isLoggedIn() || false;
  }
  
  login() {
    this.auth?.login();
  }
  
  logout() {
    this.auth?.logout();
  }
}