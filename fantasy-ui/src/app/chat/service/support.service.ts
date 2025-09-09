import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ChatRoomDTO,
  CreateSupportTicketDTO,
  SupportStatus,
  SupportType
} from '../models/support.models';

@Injectable({
  providedIn: 'root'
})
export class SupportService {
  private baseUrl = 'http://localhost:9090/fantasy/api/support';

  constructor(private http: HttpClient) { }

  // ✅ Créer un ticket de support
  createSupportTicket(ticketData: CreateSupportTicketDTO): Observable<ChatRoomDTO> {
    return this.http.post<ChatRoomDTO>(`${this.baseUrl}/ticket`, ticketData);
  }

  // ✅ Récupérer les tickets d'un utilisateur (mes tickets)
  getUserSupportTickets(): Observable<ChatRoomDTO[]> {
    return this.http.get<ChatRoomDTO[]>(`${this.baseUrl}/my-tickets`);
  }

  // ✅ Récupérer tous les tickets pour l'admin
  getAdminSupportTickets(): Observable<ChatRoomDTO[]> {
    return this.http.get<ChatRoomDTO[]>(`${this.baseUrl}/admin/tickets`);
  }

  // ✅ Dashboard admin avec stats
  getAdminSupportDashboard(): Observable<ChatRoomDTO[]> {
    return this.http.get<ChatRoomDTO[]>(`${this.baseUrl}/admin/dashboard`);
  }

  // ✅ Résoudre un ticket (admin seulement)
  resolveSupportTicket(roomId: string): Observable<{message: string}> {
    return this.http.put<{message: string}>(`${this.baseUrl}/admin/ticket/${roomId}/resolve`, {});
  }

  // ✅ Vérifier si l'utilisateur est admin
  isUserAdmin(): Observable<{isAdmin: boolean}> {
    return this.http.get<{isAdmin: boolean}>(`${this.baseUrl}/is-admin`);
  }

  // ✅ Récupérer les types de support disponibles
  getSupportTypes(): Observable<{value: string, displayName: string}[]> {
    return this.http.get<{value: string, displayName: string}[]>(`${this.baseUrl}/types`);
  }

  // ✅ Utilitaires pour l'affichage du support
  getSupportTypeDisplayName(type: SupportType | string): string {
    const displayNames: Record<SupportType, string> = {
      [SupportType.PAYMENT]: 'Paiement',
      [SupportType.TECHNICAL]: 'Problème technique',
      [SupportType.ACCOUNT]: 'Compte/Profil',
      [SupportType.GENERAL]: 'Question générale'
    };
    return displayNames[type as SupportType] ?? type;
  }

  getSupportStatusDisplayName(status: SupportStatus | string): string {
    const displayNames: Record<SupportStatus, string> = {
      [SupportStatus.OPEN]: 'Ouvert',
      [SupportStatus.IN_PROGRESS]: 'En cours',
      [SupportStatus.RESOLVED]: 'Résolu',
      [SupportStatus.CLOSED]: 'Fermé'
    };
    return displayNames[status as SupportStatus] ?? status;
  }

  // ✅ Vérifier si c'est un chat de support
  isSupportChat(room: ChatRoomDTO): boolean {
    return (room as any).isSupportChat === true;
  }

  // ✅ Obtenir la couleur du statut pour l'UI
  getSupportStatusColor(status: SupportStatus | string): string {
    const colors: Record<SupportStatus, string> = {
      [SupportStatus.OPEN]: '#ff6b6b',
      [SupportStatus.IN_PROGRESS]: '#4ecdc4',
      [SupportStatus.RESOLVED]: '#45b7d1',
      [SupportStatus.CLOSED]: '#96ceb4'
    };
    return colors[status as SupportStatus] ?? '#6c757d';
  }

  // ✅ Obtenir l'icône du type de support
  getSupportTypeIcon(type: SupportType | string): string {
    const icons: Record<SupportType, string> = {
      [SupportType.PAYMENT]: '💳',
      [SupportType.TECHNICAL]: '🔧',
      [SupportType.ACCOUNT]: '👤',
      [SupportType.GENERAL]: '❓'
    };
    return icons[type as SupportType] ?? '❓';
  }
}