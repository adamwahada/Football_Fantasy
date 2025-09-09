import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { AdminSupportService, SupportTicket, DashboardStats, StatusUpdateRequest } from './admin-support.service';

// Les interfaces sont maintenant importées du service

@Component({
  selector: 'app-admin-support-dashboard',
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-support-dashboard.component.html',
  styleUrl: './admin-support-dashboard.component.scss',
  standalone: true,
})
export class AdminSupportDashboardComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  
  // Données
  stats: DashboardStats | null = null;
  tickets: SupportTicket[] = [];
  filteredTickets: SupportTicket[] = [];
  recentTickets: SupportTicket[] = [];
  
  // États de l'interface
  loading = false;
  error: string | null = null;
  selectedTicket: SupportTicket | null = null;
  showStatusModal = false;
  showTicketDetails = false;
  
  // Filtres
  statusFilter = 'all';
  priorityFilter = 'all';
  searchQuery = '';
  
  // Formulaire de mise à jour
  statusUpdate: StatusUpdateRequest = {
    status: '',
    priority: '',
    adminNote: ''
  };
  
  // Options pour les selects
  statusOptions = [
    { value: 'OPEN', label: 'Ouvert' },
    { value: 'IN_PROGRESS', label: 'En cours' },
    { value: 'RESOLVED', label: 'Résolu' },
    { value: 'CLOSED', label: 'Fermé' }
  ];
  
  priorityOptions = [
    { value: 'LOW', label: 'Basse' },
    { value: 'MEDIUM', label: 'Moyenne' },
    { value: 'HIGH', label: 'Élevée' },
    { value: 'URGENT', label: 'Urgente' }
  ];
  
  filterOptions = [
    { value: 'all', label: 'Tous les tickets' },
    { value: 'active', label: 'Tickets actifs' },
    { value: 'urgent', label: 'Tickets urgents' },
    { value: 'closed', label: 'Tickets fermés' }
  ];

  constructor(private adminSupportService: AdminSupportService) {}

  ngOnInit() {
    this.loadDashboardData();
    this.loadTickets();
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // Charger les données du dashboard
  loadDashboardData() {
    this.loading = true;
    this.error = null;
    
    this.adminSupportService.getDashboard()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.stats = response.statistics;
          this.recentTickets = response.recentTickets || [];
          this.loading = false;
        },
        error: (error) => {
          console.error('Erreur lors du chargement du dashboard:', error);
          this.error = 'Erreur lors du chargement des données';
          this.loading = false;
        }
      });
  }

  // Charger tous les tickets
  loadTickets() {
    this.adminSupportService.getAllTickets()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.tickets = response;
          this.applyFilters();
        },
        error: (error) => {
          console.error('Erreur lors du chargement des tickets:', error);
          this.error = 'Erreur lors du chargement des tickets';
        }
      });
  }

  // Appliquer les filtres
  applyFilters() {
    let filtered = [...this.tickets];

    // Filtre par statut
    if (this.statusFilter !== 'all') {
      if (this.statusFilter === 'active') {
        filtered = filtered.filter(t => t.status === 'OPEN' || t.status === 'IN_PROGRESS');
      } else if (this.statusFilter === 'urgent') {
        filtered = filtered.filter(t => t.priority === 'URGENT' || t.priority === 'HIGH');
      } else if (this.statusFilter === 'closed') {
        filtered = filtered.filter(t => t.status === 'RESOLVED' || t.status === 'CLOSED');
      } else {
        filtered = filtered.filter(t => t.status === this.statusFilter);
      }
    }

    // Filtre par priorité
    if (this.priorityFilter !== 'all') {
      filtered = filtered.filter(t => t.priority === this.priorityFilter);
    }

    // Filtre par recherche
    if (this.searchQuery.trim()) {
      const query = this.searchQuery.toLowerCase();
      filtered = filtered.filter(t => 
        t.subject.toLowerCase().includes(query) ||
        t.description.toLowerCase().includes(query) ||
        t.ticketId.toLowerCase().includes(query) ||
        t.userName.toLowerCase().includes(query)
      );
    }

    this.filteredTickets = filtered;
  }

  // Ouvrir le modal de changement de statut
  openStatusModal(ticket: SupportTicket) {
    this.selectedTicket = ticket;
    this.statusUpdate = {
      status: ticket.status,
      priority: ticket.priority,
      adminNote: ''
    };
    this.showStatusModal = true;
  }

  // Fermer le modal
  closeStatusModal() {
    this.showStatusModal = false;
    this.selectedTicket = null;
    this.statusUpdate = {
      status: '',
      priority: '',
      adminNote: ''
    };
  }

  // Mettre à jour le statut d'un ticket
  updateTicketStatus() {
    if (!this.selectedTicket) return;

    this.loading = true;
    
    this.adminSupportService.updateTicketStatus(this.selectedTicket.ticketId, this.statusUpdate)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          console.log('Statut mis à jour:', response);
          this.closeStatusModal();
          this.loadDashboardData();
          this.loadTickets();
          this.loading = false;
        },
        error: (error) => {
          console.error('Erreur lors de la mise à jour:', error);
          this.error = 'Erreur lors de la mise à jour du statut';
          this.loading = false;
        }
      });
  }

  // Assigner un ticket à l'admin
  assignTicket(ticket: SupportTicket) {
    this.loading = true;
    
    this.adminSupportService.assignTicket(ticket.ticketId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          console.log('Ticket assigné:', response);
          this.loadDashboardData();
          this.loadTickets();
          this.loading = false;
        },
        error: (error) => {
          console.error('Erreur lors de l\'assignation:', error);
          this.error = 'Erreur lors de l\'assignation du ticket';
          this.loading = false;
        }
      });
  }

  // Ouvrir les détails d'un ticket
  openTicketDetails(ticket: SupportTicket) {
    this.selectedTicket = ticket;
    this.showTicketDetails = true;
  }

  // Fermer les détails
  closeTicketDetails() {
    this.showTicketDetails = false;
    this.selectedTicket = null;
  }

  // Ouvrir le chat d'un ticket
  openTicketChat(ticket: SupportTicket) {
    // Rediriger vers la page de chat avec l'ID de la room
    window.open(`/chat?roomId=${ticket.chatRoomId}`, '_blank');
  }

  // Obtenir la classe CSS pour le statut
  getStatusClass(status: string): string {
    switch (status) {
      case 'OPEN': return 'status-open';
      case 'IN_PROGRESS': return 'status-progress';
      case 'RESOLVED': return 'status-resolved';
      case 'CLOSED': return 'status-closed';
      default: return 'status-default';
    }
  }

  // Obtenir la classe CSS pour la priorité
  getPriorityClass(priority: string): string {
    switch (priority) {
      case 'URGENT': return 'priority-urgent';
      case 'HIGH': return 'priority-high';
      case 'MEDIUM': return 'priority-medium';
      case 'LOW': return 'priority-low';
      default: return 'priority-default';
    }
  }

  // Obtenir le libellé du statut
  getStatusLabel(status: string): string {
    const option = this.statusOptions.find(opt => opt.value === status);
    return option ? option.label : status;
  }

  // Obtenir le libellé de la priorité
  getPriorityLabel(priority: string): string {
    const option = this.priorityOptions.find(opt => opt.value === priority);
    return option ? option.label : priority;
  }

  // Formater la date
  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleString('fr-FR');
  }

  // Rafraîchir les données
  refresh() {
    this.loadDashboardData();
    this.loadTickets();
  }

  // Rechercher
  onSearch() {
    this.applyFilters();
  }

  // Changer de filtre
  onFilterChange() {
    this.applyFilters();
  }
}