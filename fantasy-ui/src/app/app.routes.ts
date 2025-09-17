import { Routes } from '@angular/router';
import { authRoutes } from './authentication/auth.routes';
import { adminRoutes } from './admin/admin.routes';
import { TestComponent } from './test/test.component';
import { userRoutes } from './user/user.routes';
import { LandingpageComponent } from './landingpage/landingpage.component';
export const routes: Routes = [
  { path: 'landingpage', component: LandingpageComponent },
  ...authRoutes,
  { path: 'admin', children: adminRoutes },
  {path: 'user', children: userRoutes }, 
  { path: 'test', component: TestComponent },
  { path: '', redirectTo: '/signin', pathMatch: 'full' },
  { path: '**', redirectTo: '/signin' }
]
