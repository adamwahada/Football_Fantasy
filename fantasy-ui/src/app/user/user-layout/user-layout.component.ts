import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';
import { UserSidebarComponent } from '../user-sidebar/user-sidebar.component';
import { UserHeaderComponent } from '../user-header/user-header.component';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-user-layout',
  standalone: true,
  imports: [RouterOutlet, UserSidebarComponent, UserHeaderComponent, CommonModule],
  templateUrl: './user-layout.component.html',
  styleUrls: ['./user-layout.component.scss']
})
export class UserLayoutComponent {
  sidebarCollapsed = false;
  sidebarHidden = false;

  constructor(private authService: AuthService) {}

  cycleSidebarState() {
    if (!this.sidebarCollapsed && !this.sidebarHidden) {
      // Full sidebar → Collapsed
      this.sidebarCollapsed = true;
    } else if (this.sidebarCollapsed && !this.sidebarHidden) {
      // Collapsed → Hidden
      this.sidebarCollapsed = false;
      this.sidebarHidden = true;
    } else if (this.sidebarHidden) {
      // Hidden → Full
      this.sidebarHidden = false;
      this.sidebarCollapsed = false;
    }
  }

  isLoggedIn(): boolean {
    return this.authService.isLoggedIn();
  }

  login() {
    this.authService.login();
  }

  logout() {
    this.authService.logout();
  }
}
