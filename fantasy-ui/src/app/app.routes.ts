import { Routes } from '@angular/router';
import { LandingComponent } from './landing.component';
import { DashboardComponent } from './dashboard.component';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  { path: 'welcome', component: LandingComponent },
  { path: 'dashboard', component: DashboardComponent },
  { path: '**', redirectTo: 'dashboard' }
];
