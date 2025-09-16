import { authGuard } from "../core/guards/auth.guard";
import { Routes } from '@angular/router';
import { UserLayoutComponent } from "./user-layout/user-layout.component";
import { gameweekAccessGuard } from "../gameweek/user-gameweek/user-gameweek-dashboard/gameweek-access.guard";
import { UserGameweekListComponent } from "../gameweek/user-gameweek/user-gameweek-dashboard/user-gameweek-list/user-gameweek-list.component";
import { UserGameweekMatchesComponent } from "../gameweek/user-gameweek/user-gameweek-dashboard/user-gameweek-matches/user-gameweek-matches.component";
import { UserGameweekDetailsComponent } from "../gameweek/user-gameweek/user-gameweek-dashboard/user-gameweek-details/user-gameweek-details.component";
import { UserGameweekClassementComponent } from "../gameweek/user-gameweek/user-gameweek-dashboard/user-gameweek-classement/user-gameweek-classement.component";
import { UserGameweekFinishedComponent } from "../gameweek/user-gameweek/user-gameweek-dashboard/user-gameweek-finished/user-gameweek-finished.component";
import { UserGameweekCurrentComponent } from "../gameweek/user-gameweek/user-gameweek-dashboard/user-gameweek-current/user-gameweek-current.component";
import { finishedGameweekGuard } from "../gameweek/user-gameweek/user-gameweek-dashboard/gameweek-access-finished.guard";
import { currentGameweekGuard } from "../gameweek/user-gameweek/user-gameweek-dashboard/gameweek-access-current.guard";
export const userRoutes: Routes = [
  {
    path: '',
    component: UserLayoutComponent,
    canActivate: [authGuard],
    data: { roles: ['ROLE_ADMIN', 'ROLE_USER'] },
    children: [
      { path: 'user-gameweek-list', component: UserGameweekListComponent },
      { path: 'user-gameweek-list/:competition', component: UserGameweekDetailsComponent, canActivate: [gameweekAccessGuard] },
      { path: 'user-gameweek-classement/:competition', component: UserGameweekClassementComponent, canActivate: [gameweekAccessGuard] },
      { path: 'user-gameweek-list/:competition/:weekNumber', component: UserGameweekMatchesComponent, canActivate: [gameweekAccessGuard] },
      { path: 'user-gameweek-list/:competition/:weekNumber/finished', component: UserGameweekFinishedComponent, canActivate: [finishedGameweekGuard] },
      { path: 'user-gameweek-list/:competition/:weekNumber/current', component: UserGameweekCurrentComponent, canActivate: [currentGameweekGuard] },
 ]}
];