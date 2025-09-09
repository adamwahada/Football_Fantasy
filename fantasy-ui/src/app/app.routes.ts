import { Routes } from '@angular/router';
import { authRoutes } from './authentication/auth.routes';
import { adminRoutes } from './admin/admin.routes';
import { TestComponent } from './test/test.component';
import { ChatPageComponent } from "./chat/chat-page/chat-page.component";
import { SupportTestComponent } from "./chat/support/support-test/support-test.component";
import {SupportTicketFormComponent} from "./chat/support/support-ticket-form/support-ticket-form.component";
import {AdminSupportDashboardComponent} from "./chat/support/admin-support-dashboard/admin-support-dashboard.component";



export const routes: Routes = [
  ...authRoutes,
  { path: 'admin', children: adminRoutes },
  { path: 'test', component: TestComponent },
  { path: 'chat', component: ChatPageComponent },
  { path: 'form', component: SupportTicketFormComponent },


  { path: 'support', component: SupportTestComponent },
  //{ path: 'admin/support', component: AdminSupportDashboardComponent },


  { path: '', redirectTo: '/signin', pathMatch: 'full' },

  //{ path: '**', redirectTo: '/signin' }
]
