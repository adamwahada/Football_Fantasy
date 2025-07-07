import { Routes } from '@angular/router';
import { authRoutes } from './authentication/auth.routes';
import { adminRoutes } from './admin/admin.routes';

export const routes: Routes = [
  ...authRoutes,
  { path: 'admin', children: adminRoutes },
  { path: '', redirectTo: '/signin', pathMatch: 'full' },
  { path: '**', redirectTo: '/signin' },

];