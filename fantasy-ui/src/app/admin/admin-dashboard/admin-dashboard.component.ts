import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService } from '../admin.service';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.scss']
})
export class AdminDashboardComponent {

  userId!: number;
  amount!: number;
  days!: number;
  banStatus: string = '';
  message: string = '';

  constructor(private adminService: AdminService) {}

  // ---------------- USER BALANCE ----------------
  creditBalance(): void {
    if (!this.userId || !this.amount) {
      this.message = '⚠️ Please enter user ID and amount.';
      return;
    }
    this.adminService.creditUserBalance(this.userId, this.amount).subscribe({
      next: res => this.message = res,
      error: err => this.message = '❌ Error: ' + err.message
    });
  }
debitBalance(): void {
  if (!this.userId || !this.amount) {
    this.message = '⚠️ Please enter user ID and amount.';
    return;
  }

  this.adminService.debitUserBalance(this.userId, this.amount).subscribe({
    next: res => this.message = res,
    error: err => {
      let backendError: any;

      // Try parsing err.error safely
      try {
        backendError = typeof err.error === 'string' ? JSON.parse(err.error) : err.error;
      } catch {
        backendError = err.error;
      }

      if (backendError?.error === 'INSUFFICIENT_BALANCE' && backendError.details) {
        const details = backendError.details;
        this.message = `❌ Insufficient balance. Required: ${details.required}, Current: ${details.current}, Shortage: ${details.shortage}`;
      } else if (backendError?.message) {
        this.message = `❌ ${backendError.message}`;
      } else {
        this.message = '❌ Something went wrong';
      }
    }
  });
}


  // ---------------- BAN MANAGEMENT ----------------
  banUserTemporarily(): void {
    if (!this.userId || !this.days) {
      this.message = '⚠️ Please enter user ID and number of days.';
      return;
    }
    this.adminService.banUserTemporarily(this.userId, this.days).subscribe({
      next: res => this.message = res,
      error: err => this.message = '❌ Error: ' + err.message
    });
  }

  banUserPermanently(): void {
    if (!this.userId) {
      this.message = '⚠️ Please enter user ID.';
      return;
    }
    this.adminService.banUserPermanently(this.userId).subscribe({
      next: res => this.message = res,
      error: err => this.message = '❌ Error: ' + err.message
    });
  }

  unbanUser(): void {
    if (!this.userId) {
      this.message = '⚠️ Please enter user ID.';
      return;
    }
    this.adminService.unbanUser(this.userId).subscribe({
      next: res => this.message = res,
      error: err => this.message = '❌ Error: ' + err.message
    });
  }

  getBanStatus(): void {
    if (!this.userId) {
      this.message = '⚠️ Please enter user ID.';
      return;
    }
    this.adminService.getUserBanStatus(this.userId).subscribe({
      next: res => this.banStatus = res,
      error: err => this.banStatus = '❌ Error: ' + err.message
    });
  }
}
