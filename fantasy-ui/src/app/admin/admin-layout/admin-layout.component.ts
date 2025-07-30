import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AdminSidebarComponent } from '../admin-sidebar/admin-sidebar.component';

@Component({
  selector: 'app-admin-layout',
  standalone: true,
  imports: [RouterOutlet, AdminSidebarComponent],
  template: `
    <div class="admin-layout">
      <app-admin-sidebar [hidden]="sidebarHidden"></app-admin-sidebar>
      <button class="sidebar-toggle" (click)="sidebarHidden = !sidebarHidden">
        {{ sidebarHidden ? '☰' : '✖' }}
      </button>
      <div class="admin-content" [class.full-width]="sidebarHidden">
        <router-outlet></router-outlet>
      </div>
    </div>
  `,
  styleUrls: ['./admin-layout.component.scss']
})
export class AdminLayoutComponent {
  sidebarHidden = false;
}