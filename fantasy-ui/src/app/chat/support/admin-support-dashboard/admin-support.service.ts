import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {environment} from "../../../../environments/environment";

export interface SupportTicket {
  id: number;
  ticketId: string;
  subject: string;
  description: string;
  supportType: string;
  status: string;
  priority: string;
  userId: number;
  userName: string;
  userEmail: string;
  assignedAdminId?: number;
  assignedAdminName?: string;
  chatRoomId: string;
  unreadMessagesCount: number;
  createdAt: string;
  updatedAt: string;
  resolvedAt?: string;
  closedAt?: string;
}

export interface DashboardStats {
  totalTickets: number;
  openTickets: number;
  inProgressTickets: number;
  resolvedTickets: number;
  closedTickets: number;
  myAssignedTickets: number;
  urgentTickets: number;
  avgResolutionTimeHours: number;
}

export interface StatusUpdateRequest {
  status: string;
  priority?: string;
  adminNote?: string;
}

@Injectable({
  providedIn: 'root'
})
export class AdminSupportService {
  private baseUrl = `${environment.apiUrl}/api/support`;

  constructor(private http: HttpClient) {}

  // Dashboard
  getDashboard(): Observable<any> {
    return this.http.get(`${this.baseUrl}/admin/dashboard`);
  }

  getDetailedStats(): Observable<any> {
    return this.http.get(`${this.baseUrl}/admin/statistics/detailed`);
  }

  // Tickets
  getAllTickets(): Observable<SupportTicket[]> {
    return this.http.get<SupportTicket[]>(`${this.baseUrl}/admin/tickets`);
  }

  getTicketsByStatus(status: string): Observable<any> {
    return this.http.get(`${this.baseUrl}/admin/tickets/status/${status}`);
  }

  getActiveTickets(): Observable<any> {
    return this.http.get(`${this.baseUrl}/admin/tickets/active`);
  }

  getClosedTickets(): Observable<any> {
    return this.http.get(`${this.baseUrl}/admin/tickets/closed`);
  }

  getUrgentTickets(): Observable<any> {
    return this.http.get(`${this.baseUrl}/admin/tickets/urgent`);
  }

  getTicketsByPriority(priority: string): Observable<any> {
    return this.http.get(`${this.baseUrl}/admin/tickets/priority/${priority}`);
  }

  searchTickets(query: string): Observable<any> {
    return this.http.get(`${this.baseUrl}/admin/tickets/search?query=${encodeURIComponent(query)}`);
  }

  // Actions sur les tickets
  updateTicketStatus(ticketId: string, request: StatusUpdateRequest): Observable<any> {
    return this.http.put(`${this.baseUrl}/admin/ticket/${ticketId}/status`, request);
  }

  assignTicket(ticketId: string): Observable<any> {
    return this.http.post(`${this.baseUrl}/admin/ticket/${ticketId}/assign`, {});
  }

  getTicketById(ticketId: string): Observable<SupportTicket> {
    return this.http.get<SupportTicket>(`${this.baseUrl}/ticket/${ticketId}`);
  }

  // Utilitaires
  getSupportTypes(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/types`);
  }

  getStatuses(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/statuses`);
  }

  getPriorities(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/priorities`);
  }

  isAdmin(): Observable<{ isAdmin: boolean }> {
    return this.http.get<{ isAdmin: boolean }>(`${this.baseUrl}/is-admin`);
  }
}
