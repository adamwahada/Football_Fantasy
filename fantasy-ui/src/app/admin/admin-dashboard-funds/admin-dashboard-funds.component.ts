import { Component, OnInit } from '@angular/core';
import { AdminService } from '../admin.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserEntity } from '../user.model';

@Component({
  selector: 'app-admin-dashboard',
  templateUrl: './admin-dashboard-funds.component.html',
  styleUrls: ['./admin-dashboard-funds.component.scss'],
  imports: [CommonModule, FormsModule],

})
export class AdminDashboardFundsComponent implements OnInit {

  users: any[] = [];
  filteredUsers: any[] = [];
  searchQuery: string = '';
  amount: number = 0;
  days: number = 0;
  message: string = '';
  refreshing = false;
  userAmounts: { [userId: number]: number } = {}; // Montant individuel par utilisateur
  constructor(private adminService: AdminService) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.refreshing = true;
    this.adminService.getAllUsers().subscribe({
      next: data => {
        this.users = data;
        this.filteredUsers = data;
        // Initialiser les montants individuels
        this.initializeUserAmounts();
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

  // Initialiser les montants individuels pour chaque utilisateur
  initializeUserAmounts(): void {
    this.userAmounts = {};
    this.users.forEach(user => {
      this.userAmounts[user.id] = 0;
    });
  }

  // Obtenir le montant pour un utilisateur spécifique
  getUserAmount(userId: number): number {
    return this.userAmounts[userId] || 0;
  }

  // Mettre à jour le montant pour un utilisateur spécifique
  updateUserAmount(userId: number, amount: number): void {
    this.userAmounts[userId] = amount;
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
    if (!this.searchQuery || this.searchQuery.trim() === '') {
      this.filteredUsers = [...this.users];
      return;
    }
    
    const query = this.searchQuery.toLowerCase().trim();
    this.filteredUsers = this.users.filter(u =>
      u.username.toLowerCase().includes(query) || 
      u.email.toLowerCase().includes(query) ||
      String(u.id).includes(query) ||
      String(u.balance).includes(query)
    );
  }

  creditBalance(userId: number): void {
    const amount = this.getUserAmount(userId);
    if (!amount || amount <= 0) {
      this.message = '⚠️ Veuillez entrer un montant valide pour cet utilisateur.';
      return;
    }
    this.adminService.creditUserBalance(userId, amount).subscribe({
      next: () => {
        this.message = `💰 Crédité ${amount}€ à l'utilisateur ${userId}`;
        this.userAmounts[userId] = 0; // Reset le montant après l'opération
        this.loadUsers();
      },
      error: err => this.message = '❌ Erreur: ' + err.message
    });
  }

  debitBalance(userId: number): void {
    const amount = this.getUserAmount(userId);
    if (!amount || amount <= 0) {
      this.message = '⚠️ Veuillez entrer un montant valide pour cet utilisateur.';
      return;
    }
    this.adminService.debitUserBalance(userId, amount).subscribe({
      next: () => {
        this.message = `💸 Débité ${amount}€ de l'utilisateur ${userId}`;
        this.userAmounts[userId] = 0; // Reset le montant après l'opération
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
