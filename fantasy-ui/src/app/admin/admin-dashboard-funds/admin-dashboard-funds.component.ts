import { Component, OnInit, ViewChild, inject } from '@angular/core';
import { AdminService } from '../admin.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatPaginator } from '@angular/material/paginator';
import { AuthService } from '../../core/services/auth.service';

interface User {
  id: number;
  username: string;
  email: string;
  balance: number;
  active: boolean;
  bannedUntil?: Date;
}

@Component({
  selector: 'app-admin-dashboard',
  templateUrl: './admin-dashboard-funds.component.html',
  styleUrls: ['./admin-dashboard-funds.component.scss'],
  imports: [CommonModule, FormsModule, MatPaginatorModule],
})
export class AdminDashboardFundsComponent implements OnInit {
  users: User[] = [];
  filteredUsers: User[] = [];
  pagedUsers: User[] = [];
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  pageSize = 10;
  pageIndex = 0;
  searchQuery: string = '';
  amount: number = 0;
  days: number = 0;
  message: string = '';
  refreshing = false;
  userAmounts: { [userId: number]: number } = {}; // Montant individuel par utilisateur
  selectedStatus: string = 'all'; // Filtre par statut

  private authService = inject(AuthService);

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  private getAdminId(): number | null {
    return this.authService.getCurrentUserId(); 
  }

  loadUsers(): void {
    this.refreshing = true;
    this.adminService.getAllUsers().subscribe({
      next: data => {
        this.users = data.map((u: any) => ({
          id: u.id,
          username: u.username ?? '',
          email: u.email ?? '',
          balance: u.balance ?? 0,
          active: u.active ?? false,
          bannedUntil: u.bannedUntil ? new Date(u.bannedUntil) : undefined
        }));
        this.initializeUserAmounts();
        this.applyFilters();
        this.updatePagedUsers();
        this.refreshing = false;
      },
      error: err => {
        this.message = '❌ Erreur lors du chargement des utilisateurs: ' + err.message;
        this.refreshing = false;
      }
    });
  }

  updatePagedUsers() {
    const start = this.pageIndex * this.pageSize;
    const end = start + this.pageSize;
    this.pagedUsers = this.filteredUsers.slice(start, end);
  }

  onPageChange(event: any) {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.updatePagedUsers();
  }

  refreshUsers(): void {
    this.message = '';
    this.loadUsers();
  }

  initializeUserAmounts(): void {
    this.userAmounts = {};
    this.users.forEach(user => {
      this.userAmounts[user.id] = 0;
    });
  }

  getUserAmount(userId: number): number {
    return this.userAmounts[userId] || 0;
  }

  updateUserAmount(userId: number, amount: number): void {
    this.userAmounts[userId] = amount;
  }

  getSearchResultsCount(): number {
    return this.filteredUsers.length;
  }

  isSearchActive(): boolean {
    return !!(this.searchQuery && this.searchQuery.trim() !== '');
  }

  isTemporarilyBanned(user: User): boolean {
    if (!user.bannedUntil) return false;
    return new Date(user.bannedUntil) > new Date();
  }

  searchUsers(): void {
    this.applyFilters();
    this.updatePagedUsers();
  }

  applyFilters(): void {
    let filtered = [...this.users];

    if (this.searchQuery && this.searchQuery.trim() !== '') {
      const query = this.searchQuery.toLowerCase().trim();
      filtered = filtered.filter(u =>
        u.username.toLowerCase().includes(query) || 
        u.email.toLowerCase().includes(query) ||
        String(u.id).includes(query) ||
        String(u.balance).includes(query)
      );
    }

    if (this.selectedStatus !== 'all') {
      filtered = filtered.filter(u => {
        switch (this.selectedStatus) {
          case 'active': return u.active && !this.isTemporarilyBanned(u);
          case 'temp-banned': return u.active && this.isTemporarilyBanned(u);
          case 'permanently-banned': return !u.active;
          default: return true;
        }
      });
    }

    this.filteredUsers = filtered;
    this.updatePagedUsers();
  }

  onStatusFilterChange(): void {
    this.applyFilters();
    this.updatePagedUsers();
  }

  creditBalance(userId: number): void {
    const amount = this.getUserAmount(userId);
    const adminId = this.getAdminId();

    if (!amount || amount <= 0) {
      this.message = '⚠️ Veuillez entrer un montant valide pour cet utilisateur.';
      return;
    }
    this.adminService.creditUserBalance(userId, amount, adminId!).subscribe({
      next: () => {
        this.message = `💰 Crédité ${amount}€ à l'utilisateur ${userId}`;
        this.userAmounts[userId] = 0;
        this.loadUsers();
      },
      error: err => this.message = '❌ Erreur: ' + err.message
    });
  }

  debitBalance(userId: number): void {
    const amount = this.getUserAmount(userId);
    const adminId = this.getAdminId();

    if (!amount || amount <= 0) {
      this.message = '⚠️ Veuillez entrer un montant valide pour cet utilisateur.';
      return;
    }
    this.adminService.debitUserBalance(userId, amount, adminId!).subscribe({
      next: () => {
        this.message = `💸 Débité ${amount}€ de l'utilisateur ${userId}`;
        this.userAmounts[userId] = 0;
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
          this.message = `❌ Solde insuffisant. Requis: ${details.required}€, Actuel: ${details.current}€, Manque: ${details.shortage}€`;
        } else if (backendError?.message) {
          this.message = `❌ ${backendError.message}`;
        } else {
          this.message = '❌ Une erreur est survenue';
        }
      }
    });
  }

  getActiveFilters(): string {
    const filters: string[] = [];

    if (this.searchQuery && this.searchQuery.trim() !== '') {
      filters.push(`Recherche: "${this.searchQuery.trim()}"`);
    }

    if (this.selectedStatus && this.selectedStatus !== 'all') {
      switch (this.selectedStatus) {
        case 'active': filters.push('Statut: Actif'); break;
        case 'temp-banned': filters.push('Statut: Temporairement banni'); break;
        case 'permanently-banned': filters.push('Statut: Définitivement banni'); break;
      }
    }

    return filters.join(' | ');
  }

  clearSearch(): void {
    this.searchQuery = '';
    this.applyFilters();
    this.updatePagedUsers();
  }

  clearStatus(): void {
    this.selectedStatus = 'all';
    this.applyFilters();
    this.updatePagedUsers();
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'active': return 'Actif';
      case 'temp-banned': return 'Temporairement banni';
      case 'permanently-banned': return 'Définitivement banni';
      default: return '';
    }
  }
}
