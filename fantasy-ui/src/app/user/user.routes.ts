import { authGuard } from "../core/guards/auth.guard";
import { Routes } from '@angular/router';
import { UserLayoutComponent } from "./user-layout/user-layout.component";
import { UserGameweekDetailsComponent } from "../gameweek/user-gameweek/user-gameweek-dashboard/user-gameweek-details/user-gameweek-details.component";
import { UserGameweekListComponent } from "../gameweek/user-gameweek/user-gameweek-dashboard/user-gameweek-list/user-gameweek-list.component";

export const userRoutes: Routes = [
  {
    path: '',
    component: UserLayoutComponent,
    canActivate: [authGuard],
    data: { roles: ['ROLE_ADMIN', 'ROLE_USER'] },
    children: [

      { path: 'user-gameweek-list', component: UserGameweekListComponent },
      { path: 'user-gameweek-list/:competition', component: UserGameweekDetailsComponent }

    ]
  }
];