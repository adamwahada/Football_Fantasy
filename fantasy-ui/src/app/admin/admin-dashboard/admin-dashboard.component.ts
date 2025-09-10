import { Component, OnInit } from '@angular/core';
import { AdminService } from '../admin.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserEntity } from '../user.model';

@Component({
  selector: 'app-admin-dashboard',
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.scss'],
  imports: [CommonModule, FormsModule],

})
export class AdminDashboardComponent implements OnInit {

  users: any[] = [];
  filteredUsers: any[] = [];
  searchQuery: string = '';
  amount: number = 0;
  days: number = 0;
  message: string = '';
  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.adminService.getAllUsers().subscribe({
      next: data => {
        this.users = data;
        this.filteredUsers = data;
      },
      error: err => this.message = '❌ Error loading users: ' + err.message
    });
  }
isTemporarilyBanned(user: UserEntity): boolean {
  if (!user.bannedUntil) return false;

  const bannedDate = new Date(user.bannedUntil); 
  return bannedDate > new Date();
}

  searchUsers(): void {
    const query = this.searchQuery.toLowerCase();
    this.filteredUsers = this.users.filter(u =>
      u.username.toLowerCase().includes(query) || 
      String(u.id).includes(query)
    );
  }

  creditBalance(userId: number): void {
    if (!this.amount || this.amount <= 0) {
      this.message = '⚠️ Enter a valid amount.';
      return;
    }
    this.adminService.creditUserBalance(userId, this.amount).subscribe({
      next: () => {
        this.message = `💰 Credited ${this.amount} to user ${userId}`;
        this.loadUsers();
      },
      error: err => this.message = '❌ Error: ' + err.message
    });
  }

  debitBalance(userId: number): void {
    if (!this.amount || this.amount <= 0) {
      this.message = '⚠️ Enter a valid amount.';
      return;
    }
    this.adminService.debitUserBalance(userId, this.amount).subscribe({
      next: () => {
        this.message = `💸 Debited ${this.amount} from user ${userId}`;
        this.loadUsers();
      },
      error: err => {
        let backendError: any;
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

  banUser(userId: number, days?: number): void {
    if (days && days > 0) {
      this.adminService.banUserTemporarily(userId, days).subscribe({
        next: () => this.message = `⏳ User ${userId} banned for ${days} days.`,
        error: err => this.message = '❌ Error: ' + err.message
      });
    } else {
      this.adminService.banUserPermanently(userId).subscribe({
        next: () => this.message = `🚫 User ${userId} banned permanently.`,
        error: err => this.message = '❌ Error: ' + err.message
      });
    }
    this.loadUsers();
  }

  unbanUser(userId: number): void {
    this.adminService.unbanUser(userId).subscribe({
      next: () => {
        this.message = `✅ User ${userId} unbanned.`;
        this.loadUsers();
      },
      error: err => this.message = '❌ Error: ' + err.message
    });
  }
}
