import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { MatSnackBarModule } from '@angular/material/snack-bar';

// Composants
import { SupportPageComponent } from './support-page/support-page.component';
import { SupportTicketFormComponent } from './support-ticket-form/support-ticket-form.component';
import { SupportButtonComponent } from './support-button/support-button.component';

// Services
import { SupportService } from '../service/support.service';

@NgModule({
  declarations: [
    SupportPageComponent,
    SupportTicketFormComponent,
    SupportButtonComponent
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatSnackBarModule
  ],
  providers: [
    SupportService
  ],
  exports: [
    SupportPageComponent,
    SupportTicketFormComponent,
    SupportButtonComponent
  ]
})
export class SupportSimpleModule { }
