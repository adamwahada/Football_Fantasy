import { Routes } from '@angular/router';
import { authRoutes } from './authentication/auth.routes';
import { adminRoutes } from './admin/admin.routes';
import { TestComponent } from './test/test.component';

export const routes: Routes = [
  ...authRoutes,
  { path: 'admin', children: adminRoutes },
  { path: 'test', component: TestComponent },
  { path: '', redirectTo: '/signin', pathMatch: 'full' },




  { path: '**', redirectTo: '/signin' }
]
