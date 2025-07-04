import { Routes } from '@angular/router';
import { authRoutes } from './authentication/auth.routes'

export const routes: Routes = [
    ...authRoutes,
    { path: '', redirectTo: '/register', pathMatch: 'full' }

];
