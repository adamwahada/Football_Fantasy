import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { UserSidebarComponent } from '../user-sidebar/user-sidebar.component';

@Component({
  selector: 'app-user-layout',
  standalone: true,
  imports: [RouterOutlet, UserSidebarComponent],
  template: `
    <div class="user-layout">
      <!-- Passe la variable sidebarCollapsed en input -->
      <app-user-sidebar [sidebarCollapsed]="sidebarCollapsed"></app-user-sidebar>

      <button class="sidebar-toggle" (click)="toggleSidebar()">
        {{ sidebarCollapsed ? '☰' : '✖' }}
      </button>

      <div class="user-content" [class.collapsed]="sidebarCollapsed">
        <router-outlet></router-outlet>
      </div>
    </div>
  `,
  styleUrls: ['./user-layout.component.scss']
})
export class UserLayoutComponent {
  sidebarCollapsed = false;

  toggleSidebar() {
    this.sidebarCollapsed = !this.sidebarCollapsed;
  }
}
