import { authGuard } from "../core/guards/auth.guard";
import { Routes } from '@angular/router';
import { ReferralCodeManagerComponent } from "./referral-code-manager/referral-code-manager.component";

export const adminRoutes: Routes = [
  { path: 'referral', component: ReferralCodeManagerComponent , canActivate: [authGuard], data: { roles: ['ROLE_ADMIN'] } },
  { path: '', redirectTo: 'referral', pathMatch: 'full' }
];