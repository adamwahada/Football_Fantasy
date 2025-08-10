import { Component, OnInit, Input } from '@angular/core';
import { RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import routesData from '../../../assets/data/routes.json';
import { KeycloakService } from '../../keycloak.service';

@Component({
  selector: 'app-user-sidebar',
  standalone: true,
  imports: [RouterModule, CommonModule],
  templateUrl: './user-sidebar.component.html',
  styleUrls: ['./user-sidebar.component.scss']
})
export class UserSidebarComponent implements OnInit {
  @Input() sidebarCollapsed = false;

  routes: any[] = [];
  allRoutes: any[] = (routesData as any).routes;  
  openIndex: number | null = null;
  userRoles: string[] = [];

  constructor(private keycloakService: KeycloakService) {}

  ngOnInit() {
    this.userRoles = this.keycloakService.getUserRoles().map(r => r.replace('ROLE_', ''));
    this.routes = this.filterRoutesForRoles(this.allRoutes, this.userRoles);
  }

  toggle(index: number) {
    this.openIndex = this.openIndex === index ? null : index;
  }

  filterRoutesForRoles(routes: any[], userRoles: string[]): any[] {
    return routes
      .filter(route => route.role.some((role: string) => userRoles.includes(role)))
      .map(route => {
        if (route.submenu && route.submenu.length) {
          route.submenu = this.filterRoutesForRoles(route.submenu, userRoles);
        }
        return route;
      });
  }
}
