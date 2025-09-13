import { Component, OnInit } from '@angular/core';
import { AdminService } from '../admin.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserEntity } from '../user.model';

@Component({
  selector: 'app-admin-dashboard-users-management',
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-dashboard-users-management.component.html',
  styleUrl: './admin-dashboard-users-management.component.scss'
})
export class AdminDashboardUsersManagementComponent implements OnInit {

  users: any[] = [];
  filteredUsers: any[] = [];
  searchQuery: string = '';
  message: string = '';
  refreshing = false;
  selectedStatus: string = 'all'; // Filtre par statut

  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.refreshing = true;
    this.adminService.getAllUsers().subscribe({
      next: data => {
        this.users = data;
        this.applyFilters();  // Reapply search + status filters
        this.refreshing = false;
      },
      error: err => {
        this.message = '❌ Erreur lors du chargement des utilisateurs: ' + err.message;
        this.refreshing = false;
      }
    });
  }

  // Méthode pour rafraîchir manuellement
  refreshUsers(): void {
    this.message = ''; // Clear any existing messages
    this.loadUsers();
  }

  // Obtenir le nombre de résultats de recherche
  getSearchResultsCount(): number {
    return this.filteredUsers.length;
  }

  // Vérifier si une recherche est active
  isSearchActive(): boolean {
    return !!(this.searchQuery && this.searchQuery.trim() !== '');
  }

  isTemporarilyBanned(user: UserEntity): boolean {
    if (!user.bannedUntil) return false;

    const bannedDate = new Date(user.bannedUntil); 
    return bannedDate > new Date();
  }

  searchUsers(): void {
    this.applyFilters();
  }

  // Appliquer les filtres de recherche et de statut
  applyFilters(): void {
    let filtered = [...this.users];

    // Filtre par recherche
    if (this.searchQuery && this.searchQuery.trim() !== '') {
      const query = this.searchQuery.toLowerCase().trim();
      filtered = filtered.filter(u =>
        u.username.toLowerCase().includes(query) || 
        u.email.toLowerCase().includes(query) ||
        String(u.id).includes(query) ||
        String(u.balance).includes(query)
      );
    }

    // Filtre par statut
    if (this.selectedStatus !== 'all') {
      filtered = filtered.filter(u => {
        switch (this.selectedStatus) {
          case 'active':
            return u.active && !this.isTemporarilyBanned(u);
          case 'temp-banned':
            return u.active && this.isTemporarilyBanned(u);
          case 'permanently-banned':
            return !u.active;
          default:
            return true;
        }
      });
    }

    this.filteredUsers = filtered;
  }

  // Changer le filtre de statut
  onStatusFilterChange(): void {
    this.applyFilters();
  }


  banUser(userId: number, days?: number): void {
    if (days && days > 0) {
      this.adminService.banUserTemporarily(userId, days).subscribe({
        next: () => {
          this.message = `⏳ Utilisateur ${userId} banni pour ${days} jour(s).`;
          this.loadUsers();
        },
        error: err => this.message = '❌ Erreur: ' + err.message
      });
    } else {
      this.adminService.banUserPermanently(userId).subscribe({
        next: () => {
          this.message = `🚫 Utilisateur ${userId} banni définitivement.`;
          this.loadUsers();
        },
        error: err => this.message = '❌ Erreur: ' + err.message
      });
    }
  }

  unbanUser(userId: number): void {
    this.adminService.unbanUser(userId).subscribe({
      next: () => {
        this.message = `✅ Utilisateur ${userId} débanni.`;
        this.loadUsers();
      },
      error: err => this.message = '❌ Erreur: ' + err.message
    });
  }
  getActiveFilters(): string {
    const filters: string[] = [];
  
    if (this.searchQuery && this.searchQuery.trim() !== '') {
      filters.push(`Recherche: "${this.searchQuery.trim()}"`);
    }
  
    if (this.selectedStatus && this.selectedStatus !== 'all') {
      switch (this.selectedStatus) {
        case 'active':
          filters.push('Statut: Actif');
          break;
        case 'temp-banned':
          filters.push('Statut: Temporairement banni');
          break;
        case 'permanently-banned':
          filters.push('Statut: Définitivement banni');
          break;
      }
    }
  
    return filters.join(' | ');
  }
  clearSearch(): void {
    this.searchQuery = '';
    this.applyFilters();
  }
  
  clearStatus(): void {
    this.selectedStatus = 'all';
    this.applyFilters();
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
