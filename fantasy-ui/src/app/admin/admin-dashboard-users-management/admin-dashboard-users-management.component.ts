import { Component, OnInit, ViewChild, AfterViewInit, inject } from '@angular/core';
import { AdminService } from '../admin.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatPaginator } from '@angular/material/paginator';
import { MatPaginatorModule } from '@angular/material/paginator';
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
  selector: 'app-admin-dashboard-users-management',
  imports: [CommonModule, FormsModule, MatPaginatorModule],
  templateUrl: './admin-dashboard-users-management.component.html',
  styleUrl: './admin-dashboard-users-management.component.scss'
})
export class AdminDashboardUsersManagementComponent implements OnInit, AfterViewInit {

  users: User[] = [];
  filteredUsers: User[] = [];
  searchQuery: string = '';
  message: string = '';
  refreshing = false;
  selectedStatus: string = 'all';
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  pageSize = 10;
  pageIndex = 0;
  pagedUsers: User[] = [];

  private authService = inject(AuthService);

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  ngAfterViewInit() {
    this.updatePagedUsers();
  }

  private getAdminId(): number | null {
    return this.authService.getCurrentUserId(); 
  }

  // ================= USERS =================
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

  refreshUsers(): void {
    this.message = '';
    this.loadUsers();
  }

  // ================= PAGINATION =================
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

  // ================= FILTERS =================
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
  }

  onStatusFilterChange(): void {
    this.applyFilters();
    this.updatePagedUsers();
  }

  // ================= ADMIN ACTIONS =================
  private requireAdminId(): number | null {
    const adminId = this.getAdminId();
    if (!adminId) {
      this.message = '❌ Impossible de récupérer l’ID de l’admin connecté.';
      return null;
    }
    return adminId;
  }

  banUser(userId: number, days?: number): void {
    const adminId = this.requireAdminId();
    if (!adminId) return;

    if (days && days > 0) {
      this.adminService.banUserTemporarily(userId, days, adminId).subscribe({
        next: () => {
          this.message = `⏳ Utilisateur ${userId} banni pour ${days} jour(s).`;
          this.loadUsers();
        },
        error: err => this.message = '❌ Erreur: ' + err.message
      });
    } else {
      this.adminService.banUserPermanently(userId, adminId).subscribe({
        next: () => {
          this.message = `🚫 Utilisateur ${userId} banni définitivement.`;
          this.loadUsers();
        },
        error: err => this.message = '❌ Erreur: ' + err.message
      });
    }
  }

  unbanUser(userId: number): void {
    const adminId = this.requireAdminId();
    if (!adminId) return;

    this.adminService.unbanUser(userId, adminId).subscribe({
      next: () => {
        this.message = `✅ Utilisateur ${userId} débanni.`;
        this.loadUsers();
      },
      error: err => this.message = '❌ Erreur: ' + err.message
    });
  }

  creditUser(userId: number, amount: number): void {
    const adminId = this.requireAdminId();
    if (!adminId) return;

    this.adminService.creditUserBalance(userId, amount, adminId).subscribe({
      next: () => {
        this.message = `💰 Utilisateur ${userId} crédité de ${amount}.`;
        this.loadUsers();
      },
      error: err => this.message = '❌ Erreur: ' + err.message
    });
  }

  debitUser(userId: number, amount: number): void {
    const adminId = this.requireAdminId();
    if (!adminId) return;

    this.adminService.debitUserBalance(userId, amount, adminId).subscribe({
      next: () => {
        this.message = `💸 Utilisateur ${userId} débité de ${amount}.`;
        this.loadUsers();
      },
      error: err => this.message = '❌ Erreur: ' + err.message
    });
  }

  // ================= FILTER DISPLAY =================
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
