import { Component, OnInit, ViewChild, AfterViewInit, inject } from '@angular/core';
import { AdminService } from '../admin.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatPaginator } from '@angular/material/paginator';
import { MatPaginatorModule } from '@angular/material/paginator';
import { AuthService } from '../../core/services/auth.service';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';


interface User {
  id: number;
  username: string;
  email: string;
  balance: number;
  active: boolean;
  bannedUntil?: Date;
}
export enum BanCause {
  CHEATING = 'CHEATING',
  SPAM = 'SPAM',
  HARASSMENT = 'HARASSMENT',
  INAPPROPRIATE_CONTENT = 'INAPPROPRIATE_CONTENT',
  MULTIPLE_ACCOUNTS = 'MULTIPLE_ACCOUNTS',
  PAYMENT_FRAUD = 'PAYMENT_FRAUD',
  SECURITY_THREAT = 'SECURITY_THREAT',
  VIOLATION_OF_RULES = 'VIOLATION_OF_RULES',
  OTHER = 'OTHER'
}

@Component({
  selector: 'app-admin-dashboard-users-management',
  imports: [CommonModule, FormsModule, MatPaginatorModule, MatButtonModule, MatTooltipModule, MatIconModule],
  templateUrl: './admin-dashboard-users-management.component.html',
  styleUrl: './admin-dashboard-users-management.component.scss'
})
export class AdminDashboardUsersManagementComponent implements OnInit, AfterViewInit {
  public BanCause = BanCause;

  users: User[] = [];
  filteredUsers: User[] = [];
  searchQuery: string = '';
  refreshing = false;
  selectedStatus: string = 'all';
  @ViewChild(MatPaginator) paginator!: MatPaginator;
  pageSize = 10;
  pageIndex = 0;
  pagedUsers: User[] = [];

  private authService = inject(AuthService);
  private snackBar = inject(MatSnackBar);

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
        this.showMessage('❌ Erreur lors du chargement des utilisateurs: ' + err.message);
        this.refreshing = false;
      }
    });
  }

  refreshUsers(): void {
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
private requireAdminId(): number {
  const adminId = this.getAdminId();
  if (!adminId) {
    this.showMessage('❌ Impossible de récupérer l’ID de l’admin connecté.');
    throw new Error('Admin ID missing');
  }
  return adminId;
}


banUser(userId: number, days?: number, reason?: BanCause,username?: string): void {
  const adminId = this.requireAdminId();
  if (!adminId) return;

  if (!reason) {
    this.showMessage('⚠️ Veuillez sélectionner une raison pour le bannissement.');
    return;
  }

  if (days && days > 0) {
    this.adminService.banUserTemporarily(userId, days, adminId, reason).subscribe({
      next: () => {
        this.showMessage(`⏳  ${username} banni pour ${days} jour(s) (${reason}).`);
        this.loadUsers();
      },
      error: err => this.showMessage('❌ Erreur: ' + err.message)
    });
  } else {
    this.adminService.banUserPermanently(userId, adminId, reason).subscribe({
      next: () => {
        this.showMessage(`🚫 ${username} banni définitivement (${reason}).`);
        this.loadUsers();
      },
      error: err => this.showMessage('❌ Erreur: ' + err.message)
    });
  }
}

unbanUser(userId: number, username?: string): void {
  const adminId = this.requireAdminId();
  if (!adminId) return;

  this.adminService.unbanUser(userId, adminId).subscribe({
    next: () => {
      this.showMessage(`✅ Utilisateur: ${username ?? userId} débanni.`);
      this.loadUsers();
    },
    error: err => {
      this.showMessage(`❌ Erreur lors du débannissement de ${username ?? userId} : ${err.message}`);
    }
  });
}


  creditUser(userId: number, amount: number, username: string): void {
    const adminId = this.requireAdminId();
    if (!adminId) return;

    this.adminService.creditUserBalance(userId, amount, adminId).subscribe({
      next: () => {
        this.showMessage(`💰 ${username} crédité de ${amount}.`);
        this.loadUsers();
      },
      error: err => this.showMessage('❌ Erreur: ' + err.message)
    });
  }

  debitUser(userId: number, amount: number, username: string): void {
    const adminId = this.requireAdminId();
    if (!adminId) return;

    this.adminService.debitUserBalance(userId, amount, adminId).subscribe({
      next: () => {
        this.showMessage(`💸  ${username} débité de ${amount}.`);
        this.loadUsers();
      },
      error: err => this.showMessage('❌ Erreur: ' + err.message)
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

  // ================= BAN DIALOG =================
  banDropdownUserId: number | null = null;
  banDropdownDays: number | null = null;
  selectedBanReason: BanCause | null = null;
  banDropdownAnchor: HTMLElement | null = null;

  // Call this when a ban button is clicked
  openBanDropdown(userId: number, days?: number, event?: MouseEvent): void {
    this.banDropdownUserId = userId;
    this.banDropdownDays = days ?? null;
    this.selectedBanReason = null;
    if (event) {
      this.banDropdownAnchor = event.target as HTMLElement;
    }
  }

  confirmBanDropdown(): void {
    if (!this.banDropdownUserId || !this.selectedBanReason) return;
    const userId = this.banDropdownUserId;
    const reason = this.selectedBanReason;
    const username = this.users.find(u => u.id === userId)?.username || 'Utilisateur';
    if (this.banDropdownDays && this.banDropdownDays > 0) {
      this.adminService.banUserTemporarily(userId, this.banDropdownDays, this.requireAdminId(), reason)
        .subscribe({
          next: () => {
            this.showMessage(`⏳ ${username} banni pour ${this.banDropdownDays} jour(s) (${reason}).`);
            this.loadUsers();
            this.closeBanDropdown();
          },
          error: err => this.showMessage('❌ Erreur: ' + err.message)
        });
    } else {
      this.adminService.banUserPermanently(userId, this.requireAdminId(), reason)
        .subscribe({
          next: () => {
            this.showMessage(`🚫 ${username} banni définitivement (${reason}).`);
            this.loadUsers();
            this.closeBanDropdown();
          },
          error: err => this.showMessage('❌ Erreur: ' + err.message)
        });
    }
  }

  closeBanDropdown(): void {
    this.banDropdownUserId = null;
    this.banDropdownDays = null;
    this.selectedBanReason = null;
    this.banDropdownAnchor = null;
  }

  private showMessage(message: string) {
    let panelClass = 'snackbar-info';
    if (message.includes('débanni')) panelClass = 'snackbar-success';
    if (message.includes('Erreur') || message.includes('❌')) panelClass = 'snackbar-error';
    if (message.includes('⚠️')) panelClass = 'snackbar-warning';
    this.snackBar.open(message, 'Fermer', {
      duration: 5000,
      panelClass: [panelClass]
    });
  }
}
