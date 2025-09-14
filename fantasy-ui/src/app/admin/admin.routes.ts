import { authGuard } from "../core/guards/auth.guard";
import { Routes } from '@angular/router';
import { ReferralCodeManagerComponent } from "./referral-code-manager/referral-code-manager.component";
import { AdminLayoutComponent } from "./admin-layout/admin-layout.component";
import { EditAdminMatchComponent } from "../match/admin-match/edit-admin-match/edit-admin-match.component";
import { AddAdminMatchComponent } from "../match/admin-match/add-admin-match/add-admin-match.component";
import { AllAdminMatchComponent } from "../match/admin-match/all-admin-match/all-admin-match.component";
import { AddAdminGameweekComponent } from "../gameweek/admin-gameweek/add-admin-gameweek/add-admin-gameweek.component";
import { AllAdminGameweekComponent } from "../gameweek/admin-gameweek/all-admin-gameweek/all-admin-gameweek.component";
import { EditAdminGameweekComponent } from "../gameweek/admin-gameweek/edit-admin-gameweek/edit-admin-gameweek.component";
import { AdminDashboardFundsComponent } from "./admin-dashboard-funds/admin-dashboard-funds.component";
import { AdminDashboardUsersManagementComponent } from "./admin-dashboard-users-management/admin-dashboard-users-management.component";
import { AdminDashboardHistoryComponent } from "./admin-dashboard-history/admin-dashboard-history.component";

export const adminRoutes: Routes = [
  {
    path: '',
    component: AdminLayoutComponent,
    canActivate: [authGuard],
    data: { roles: ['ROLE_ADMIN'] },
    children: [
      { path: 'referral', component: ReferralCodeManagerComponent },
      { path: 'Addmatch', component: AddAdminMatchComponent },
      { path: 'Allmatch', component: AllAdminMatchComponent },
      { path: 'Allmatch/select/:gameweekId', component: AllAdminMatchComponent }, 
      { path: 'match/Editmatch/:id', component: EditAdminMatchComponent },
      { path: 'AddGameweek', component :AddAdminGameweekComponent},
      { path: 'allgameweek', component: AllAdminGameweekComponent },
      { path: 'gameweek/Editgameweek/:id', component: EditAdminGameweekComponent },
      {path: 'funds', component: AdminDashboardFundsComponent },
      {path: 'management', component: AdminDashboardUsersManagementComponent },
      {path: 'admin-dashboard-history', component: AdminDashboardHistoryComponent },
      { path: '', redirectTo: 'referral', pathMatch: 'full' },

    ]
  }
];