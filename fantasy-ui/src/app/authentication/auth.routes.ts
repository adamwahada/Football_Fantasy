import { Routes } from '@angular/router';
import { SigninComponent } from './signin/signin.component';
import { RegistrationComponent } from './registration/registration.component';
import { UnauthorizedComponent } from './unauthorized/unauthorized.component';

export const authRoutes: Routes = [
  { path: 'signin', component: SigninComponent },
  { path: 'register', component: RegistrationComponent },
  { path: 'unauthorized', component: UnauthorizedComponent }
];