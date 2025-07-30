import { authGuard } from "../core/guards/auth.guard";
import { Routes } from '@angular/router';
import { ReferralCodeManagerComponent } from "./referral-code-manager/referral-code-manager.component";
import { AdminMatchComponent } from "../match/admin-match/admin-match.component";

export const adminRoutes: Routes = [
  { path: 'referral', component: ReferralCodeManagerComponent , canActivate: [authGuard], data: { roles: ['ROLE_ADMIN'] } },
  { path: 'match', component: AdminMatchComponent, canActivate: [authGuard], data: { roles: ['ROLE_ADMIN'] } },
  { path: '', redirectTo: 'referral', pathMatch: 'full' },
];