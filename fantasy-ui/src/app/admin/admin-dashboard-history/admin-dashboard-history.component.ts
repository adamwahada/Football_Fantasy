import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AdminService, BanCause, UserAction, UserManagementAuditDTO } from '../admin.service';

interface MappedAuditRecord extends UserManagementAuditDTO {
  username: string;
  email: string;
  adminUsername: string;
  formattedDate: string;
}

@Component({
  selector: 'app-admin-dashboard-history',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatPaginatorModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule
  ],
  templateUrl: './admin-dashboard-history.component.html',
  styleUrl: './admin-dashboard-history.component.scss'
})
export class AdminDashboardHistoryComponent implements OnInit {
  
  // Data
  allActions: MappedAuditRecord[] = [];
  filteredActions: MappedAuditRecord[] = [];
  
  // Separated data for two tables
  financialActions: MappedAuditRecord[] = [];
  moderationActions: MappedAuditRecord[] = [];
  
  // Pagination for each table
  pagedFinancialActions: MappedAuditRecord[] = [];
  pagedModerationActions: MappedAuditRecord[] = [];
  
  // Pagination settings
  financialPageSize: number = 10;
  moderationPageSize: number = 10;
  financialCurrentPage: number = 0;
  moderationCurrentPage: number = 0;
  
  // Search and filters
  searchQuery: string = '';
  selectedReason: BanCause | 'all' = 'all';
  selectedAction: UserAction | 'all' = 'all';
  selectedDateRange: 'all' | 'today' | 'week' | 'month' = 'all';
  
  // Separate filters for each table
  selectedFinancialAction: UserAction | 'all' = 'all';
  selectedModerationAction: UserAction | 'all' = 'all';
  selectedModerationReason: BanCause | 'all' = 'all';
  
  // Legacy pagination (kept for compatibility)
  pageSize: number = 25;
  currentPage: number = 0;
  
  // Loading state
  loading: boolean = false;
  refreshing: boolean = false;
  
  // Enums for template
  BanCause = BanCause;
  UserAction = UserAction;

  constructor(
    private adminService: AdminService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadAllActions();
  }

  loadAllActions(): void {
    this.loading = true;
    
    this.adminService.getAllAuditRecords().subscribe({
      next: (auditRecords) => {
        this.allActions = auditRecords.map(record => ({
          ...record,
          username: record.username ?? `User ${record.userId}`,
          email: record.email ?? 'N/A',
          adminUsername: record.adminUsername ?? `Admin ${record.adminId}`,
          formattedDate: record.formattedDate ?? this.formatDateFromTimestamp(record.timestamp)
        } as MappedAuditRecord));
        this.separateActionsByType();
        this.applyFilters();
        this.loading = false;
        this.showMessage('✅ Historique chargé avec succès');
      },
      error: (error) => {
        console.error('Erreur lors du chargement de l\'historique:', error);
        this.loading = false;
        this.showMessage('❌ Erreur lors du chargement de l\'historique');
      }
    });
  }

  private formatDateFromTimestamp(timestamp: string): string {
    try {
      const date = new Date(timestamp);
      return date.toLocaleDateString('fr-FR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      }).replace(',', '');
    } catch (error) {
      return timestamp;
    }
  }

  refreshActions(): void {
    this.refreshing = true;
    setTimeout(() => {
      this.loadAllActions();
      this.refreshing = false;
    }, 1000);
  }

  // Search functionality
  searchActions(): void {
    this.applyFilters();
  }

  // Filter functionality
  onReasonFilterChange(): void {
    this.applyFilters();
  }

  onActionFilterChange(): void {
    this.applyFilters();
  }

  onDateRangeChange(): void {
    this.applyFilters();
  }

  // Financial table filter handlers
  onFinancialActionFilterChange(): void {
    this.financialCurrentPage = 0;
    this.separateActionsByType();
  }

  // Moderation table filter handlers
  onModerationActionFilterChange(): void {
    this.moderationCurrentPage = 0;
    this.separateActionsByType();
  }

  onModerationReasonFilterChange(): void {
    this.moderationCurrentPage = 0;
    this.separateActionsByType();
  }

  applyFilters(): void {
    let filtered = [...this.allActions];

    // Apply search filter
    if (this.searchQuery && this.searchQuery.trim()) {
      const query = this.searchQuery.toLowerCase();
      filtered = filtered.filter(action => 
        action.username.toLowerCase().includes(query) ||
        action.email.toLowerCase().includes(query) ||
        action.adminUsername.toLowerCase().includes(query) ||
        action.userId.toString().includes(query) ||
        action.adminId.toString().includes(query)
      );
    }

    // Apply reason filter
    if (this.selectedReason && this.selectedReason !== 'all') {
      filtered = filtered.filter(action => action.reason === this.selectedReason);
    }

    // Apply action filter
    if (this.selectedAction && this.selectedAction !== 'all') {
      filtered = filtered.filter(action => action.action === this.selectedAction);
    }

    // Apply date range filter
    if (this.selectedDateRange !== 'all') {
      const now = new Date();
      const filterDate = new Date();
      
      switch (this.selectedDateRange) {
        case 'today':
          filterDate.setHours(0, 0, 0, 0);
          break;
        case 'week':
          filterDate.setDate(now.getDate() - 7);
          break;
        case 'month':
          filterDate.setMonth(now.getMonth() - 1);
          break;
      }
      
      filtered = filtered.filter(action => 
        new Date(action.timestamp) >= filterDate
      );
    }

    // Sort by most recent first
    filtered.sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());

    this.filteredActions = filtered;
    this.separateActionsByType();
    this.currentPage = 0;
    this.financialCurrentPage = 0;
    this.moderationCurrentPage = 0;
    this.updatePagedActions();
  }

  // Clear filters
  clearSearch(): void {
    this.searchQuery = '';
    this.applyFilters();
  }

  clearFinancialFilter(): void {
    this.selectedFinancialAction = 'all';
    this.separateActionsByType();
  }

  clearModerationActionFilter(): void {
    this.selectedModerationAction = 'all';
    this.separateActionsByType();
  }

  clearModerationReasonFilter(): void {
    this.selectedModerationReason = 'all';
    this.separateActionsByType();
  }

  clearDateFilter(): void {
    this.selectedDateRange = 'all';
    this.applyFilters();
  }

  clearAllFilters(): void {
    this.searchQuery = '';
    this.selectedDateRange = 'all';
    this.selectedFinancialAction = 'all';
    this.selectedModerationAction = 'all';
    this.selectedModerationReason = 'all';
    this.applyFilters();
  }

  // Pagination handlers
  onFinancialPageChange(event: PageEvent): void {
    this.financialPageSize = event.pageSize;
    this.financialCurrentPage = event.pageIndex;
    this.updatePagedActions();
  }
  
  onModerationPageChange(event: PageEvent): void {
    this.moderationPageSize = event.pageSize;
    this.moderationCurrentPage = event.pageIndex;
    this.updatePagedActions();
  }
  
  // Legacy pagination (kept for compatibility)
  onPageChange(event: PageEvent): void {
    this.pageSize = event.pageSize;
    this.currentPage = event.pageIndex;
    this.updatePagedActions();
  }

  // Separate actions into financial and moderation categories
  separateActionsByType(): void {
    // Filter financial actions
    let financialFiltered = this.filteredActions.filter(action => 
      action.action === UserAction.CREDIT || action.action === UserAction.DEBIT
    );
    
    // Apply financial action filter
    if (this.selectedFinancialAction !== 'all') {
      financialFiltered = financialFiltered.filter(action => action.action === this.selectedFinancialAction);
    }
    
    this.financialActions = financialFiltered;
    
    // Filter moderation actions
    let moderationFiltered = this.filteredActions.filter(action => 
      action.action === UserAction.BAN || 
      action.action === UserAction.TEMP_BAN || 
      action.action === UserAction.PERMANENT_BAN || 
      action.action === UserAction.UNBAN
    );
    
    // Apply moderation action filter
    if (this.selectedModerationAction !== 'all') {
      moderationFiltered = moderationFiltered.filter(action => action.action === this.selectedModerationAction);
    }
    
    // Apply moderation reason filter
    if (this.selectedModerationReason !== 'all') {
      moderationFiltered = moderationFiltered.filter(action => action.reason === this.selectedModerationReason);
    }
    
    this.moderationActions = moderationFiltered;
    
    this.updatePagedActions();
  }

  updatePagedActions(): void {
    // Update financial actions pagination
    const financialStartIndex = this.financialCurrentPage * this.financialPageSize;
    const financialEndIndex = financialStartIndex + this.financialPageSize;
    this.pagedFinancialActions = this.financialActions.slice(financialStartIndex, financialEndIndex);
    
    // Update moderation actions pagination
    const moderationStartIndex = this.moderationCurrentPage * this.moderationPageSize;
    const moderationEndIndex = moderationStartIndex + this.moderationPageSize;
    this.pagedModerationActions = this.moderationActions.slice(moderationStartIndex, moderationEndIndex);
    
    // Legacy pagination (kept for compatibility)
    const startIndex = this.currentPage * this.pageSize;
    const endIndex = startIndex + this.pageSize;
    // Note: pagedActions is no longer used in template but kept for compatibility
  }

  // Utility methods
  isSearchActive(): boolean {
    return !!(this.searchQuery && this.searchQuery.trim());
  }

  hasActiveFilters(): boolean {
    return this.isSearchActive() || 
           this.selectedDateRange !== 'all' ||
           this.selectedFinancialAction !== 'all' ||
           this.selectedModerationAction !== 'all' ||
           this.selectedModerationReason !== 'all';
  }

  getSearchResultsCount(): number {
    return this.filteredActions.length;
  }

  getReasonLabel(reason: BanCause): string {
    const labels: { [key in BanCause]: string } = {
      [BanCause.CHEATING]: 'Triche',
      [BanCause.SPAM]: 'Spam',
      [BanCause.HARASSMENT]: 'Harcèlement',
      [BanCause.INAPPROPRIATE_CONTENT]: 'Contenu inapproprié',
      [BanCause.MULTIPLE_ACCOUNTS]: 'Multi-comptes',
      [BanCause.PAYMENT_FRAUD]: 'Fraude de paiement',
      [BanCause.SECURITY_THREAT]: 'Menace sécurité',
      [BanCause.VIOLATION_OF_RULES]: 'Violation des règles',
      [BanCause.OTHER]: 'Autre'
    };
    return labels[reason];
  }

  getActionLabel(action: UserAction): string {
    const labels: { [key in UserAction]: string } = {
      [UserAction.BAN]: 'Bannissement',
      [UserAction.TEMP_BAN]: 'Ban temporaire',
      [UserAction.PERMANENT_BAN]: 'Ban définitif',
      [UserAction.CREDIT]: 'Montant Deposit',
      [UserAction.DEBIT]: 'Montant Withdraw',
      [UserAction.UNBAN]: 'Unban'
    };
    return labels[action];
  }

  getActionIcon(action: UserAction): string {
    const icons: { [key in UserAction]: string } = {
      [UserAction.BAN]: 'gavel',
      [UserAction.TEMP_BAN]: 'schedule',
      [UserAction.PERMANENT_BAN]: 'block',
      [UserAction.CREDIT]: 'add_circle',
      [UserAction.DEBIT]: 'remove_circle',
      [UserAction.UNBAN]: 'check_circle'
    };
    return icons[action];
  }

  getActionColor(action: UserAction): string {
    const colors: { [key in UserAction]: string } = {
      [UserAction.BAN]: 'red',
      [UserAction.TEMP_BAN]: 'orange',
      [UserAction.PERMANENT_BAN]: 'dark-red',
      [UserAction.CREDIT]: 'green',
      [UserAction.DEBIT]: 'red',
      [UserAction.UNBAN]: 'light-green'
    };
    return colors[action];
  }

  getDateRangeLabel(range: string): string {
    const labels: { [key: string]: string } = {
      'today': "Aujourd'hui",
      'week': '7 derniers jours',
      'month': '30 derniers jours',
      'all': 'Toutes les dates'
    };
    return labels[range] || range;
  }

  formatActionDetails(action: MappedAuditRecord): string {
    // For financial actions, extract amount with symbol
    if (action.action === UserAction.CREDIT || action.action === UserAction.DEBIT) {
      let amount = 0;
      
      // If we have specific amount from the DTO
      if (action.amount) {
        amount = action.amount;
      } else if (action.details) {
        // Otherwise, try to extract from details field
        const amountMatch = action.details.match(/(\d+\.?\d*)/);
        if (amountMatch) {
          amount = parseFloat(amountMatch[1]);
        }
      }
      
      if (amount > 0) {
        const symbol = action.action === UserAction.CREDIT ? '+' : '-';
        return `${symbol}${amount.toFixed(2)}€`;
      }
      
      return 'N/A';
    }
    
    // For moderation actions, extract days or other details
    if (action.days) {
      return `${action.days} jour${action.days > 1 ? 's' : ''}`;
    }
    
    if (action.details) {
      // Extract days from details (e.g., "Banned for 7 day(s)")
      const daysMatch = action.details.match(/(\d+)\s+day/);
      if (daysMatch && action.action === UserAction.TEMP_BAN) {
        const days = parseInt(daysMatch[1]);
        return `${days} jour${days > 1 ? 's' : ''}`;
      }
      
      // Return details as is if no specific pattern found
      return action.details;
    }
    
    return '';
  }

  getBalanceBefore(action: MappedAuditRecord): string {
    const amount = this.getActionAmount(action);
    
    // Try to extract balance information from details
    if (action.details) {
      // Look for patterns like "Previous balance: 100.0" or "Old balance: 100.0"
      const beforeMatch = action.details.match(/(?:previous|old|before)[\s\w]*balance[:\s]+(\d+\.?\d*)/i);
      if (beforeMatch) {
        return parseFloat(beforeMatch[1]).toFixed(2);
      }
      
      // If we have new balance and amount, calculate previous
      const newMatch = action.details.match(/(?:new|current)[\s\w]*balance[:\s]+(\d+\.?\d*)/i);
      if (newMatch && amount) {
        const newBalance = parseFloat(newMatch[1]);
        const beforeBalance = action.action === UserAction.CREDIT ? newBalance - amount : newBalance + amount;
        return beforeBalance.toFixed(2);
      }
    }
    
    return '0.00';
  }

  getBalanceAfter(action: MappedAuditRecord): string {
    const amount = this.getActionAmount(action);
    
    // Try to extract balance information from details
    if (action.details) {
      // Look for patterns like "New balance: 150.0" or "Current balance: 150.0"
      const afterMatch = action.details.match(/(?:new|current)[\s\w]*balance[:\s]+(\d+\.?\d*)/i);
      if (afterMatch) {
        return parseFloat(afterMatch[1]).toFixed(2);
      }
      
      // If we have previous balance and amount, calculate new
      const beforeMatch = action.details.match(/(?:previous|old|before)[\s\w]*balance[:\s]+(\d+\.?\d*)/i);
      if (beforeMatch && amount) {
        const beforeBalance = parseFloat(beforeMatch[1]);
        const afterBalance = action.action === UserAction.CREDIT ? beforeBalance + amount : beforeBalance - amount;
        return afterBalance.toFixed(2);
      }
    }
    
    return '0.00';
  }

  private getActionAmount(action: MappedAuditRecord): number | null {
    if (action.amount) {
      return action.amount;
    }
    
    if (action.details) {
      const amountMatch = action.details.match(/(\d+\.?\d*)/);
      if (amountMatch && (action.action === UserAction.CREDIT || action.action === UserAction.DEBIT)) {
        return parseFloat(amountMatch[1]);
      }
    }
    
    return null;
  }

  private showMessage(message: string): void {
    let panelClass = 'snackbar-info';
    if (message.includes('✅')) panelClass = 'snackbar-success';
    if (message.includes('❌')) panelClass = 'snackbar-error';
    if (message.includes('⚠️')) panelClass = 'snackbar-warning';
    
    this.snackBar.open(message, 'Fermer', {
      duration: 5000,
      panelClass: [panelClass]
    });
  }
}