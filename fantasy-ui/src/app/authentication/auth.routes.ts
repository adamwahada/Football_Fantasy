import { Routes } from '@angular/router';
import { SigninComponent } from './signin/signin.component';
import { RegistrationComponent } from './registration/registration.component';
import { UnauthorizedComponent } from './unauthorized/unauthorized.component';
import { EmailVerificationComponent } from './email-verification/email-verification.component';
import { TestComponent } from '../test/test.component';

export const authRoutes: Routes = [
  { path: 'signin', component: SigninComponent },
  { path: 'register', component: RegistrationComponent },
  { path: 'unauthorized', component: UnauthorizedComponent },
  {path: 'email-verification', component: EmailVerificationComponent},
  {path: 'test', component: TestComponent}

];