// chat.service.ts - Version mise à jour avec support
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ChatMessageDTO,
  ChatParticipantDTO,
  ChatRoomDTO,
  CreateGroupDTO, CreateSupportTicketDTO,
  SendMessageDTO, SupportStatus, SupportType
} from "../models/chat.models";

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private baseUrl = 'http://localhost:9090/fantasy/api/chat';

  constructor(private http: HttpClient) { }

  // ========== MÉTHODES CHAT EXISTANTES ==========
  getUserChats(): Observable<ChatRoomDTO[]> {
    return this.http.get<ChatRoomDTO[]>(`${this.baseUrl}/rooms`);
  }

  createOrGetPrivateChat(otherUserId: number): Observable<ChatRoomDTO> {
    return this.http.post<ChatRoomDTO>(`${this.baseUrl}/rooms/private/${otherUserId}`, {});
  }

  createGroup(groupData: CreateGroupDTO): Observable<ChatRoomDTO> {
    return this.http.post<ChatRoomDTO>(`${this.baseUrl}/rooms/group`, groupData);
  }

  sendMessage(messageData: SendMessageDTO): Observable<ChatMessageDTO> {
    return this.http.post<ChatMessageDTO>(`${this.baseUrl}/messages`, messageData);
  }

  uploadFile(roomId: string, file: File): Observable<ChatMessageDTO> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ChatMessageDTO>(`${this.baseUrl}/rooms/${roomId}/files`, formData);
  }

  getRoomMessages(roomId: string, page: number = 0, size: number = 50): Observable<any> {
    const params = new HttpParams()
        .set('page', page.toString())
        .set('size', size.toString());
    return this.http.get<any>(`${this.baseUrl}/rooms/${roomId}/messages`, { params });
  }

  markAsRead(messageId: number, roomId: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/messages/${messageId}/read?roomId=${roomId}`, {});
  }

  searchMessages(roomId: string, query: string): Observable<ChatMessageDTO[]> {
    const params = new HttpParams().set('query', query);
    return this.http.get<ChatMessageDTO[]>(`${this.baseUrl}/rooms/${roomId}/search`, { params });
  }

  editMessage(messageId: number, newContent: string): Observable<ChatMessageDTO> {
    return this.http.put<ChatMessageDTO>(`${this.baseUrl}/messages/${messageId}`,
        { content: newContent },
        { headers: { 'Content-Type': 'application/json' } }
    );
  }

  deleteMessage(messageId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/messages/${messageId}`);
  }

  getActiveParticipants(roomId: string): Observable<ChatParticipantDTO[]> {
    return this.http.get<ChatParticipantDTO[]>(`${this.baseUrl}/rooms/${roomId}/participants`);
  }

  addParticipants(roomId: string, userIds: number[]): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/rooms/${roomId}/participants`, userIds);
  }

  removeParticipant(roomId: string, participantId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/rooms/${roomId}/participants/${participantId}`);
  }

  leaveGroup(roomId: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/rooms/${roomId}/leave`, {});
  }

  updateGroupInfo(roomId: string, updateData: CreateGroupDTO): Observable<ChatRoomDTO> {
    return this.http.put<ChatRoomDTO>(`${this.baseUrl}/rooms/${roomId}`, updateData);
  }

  // ========== NOUVELLES MÉTHODES SUPPORT ==========

  // ✅ Créer un ticket de support
  createSupportTicket(ticketData: CreateSupportTicketDTO): Observable<ChatRoomDTO> {
    return this.http.post<ChatRoomDTO>(`http://localhost:9090/api/support/ticket`, ticketData);
  }

  // ✅ Récupérer les tickets d'un utilisateur (mes tickets)
  getUserSupportTickets(): Observable<ChatRoomDTO[]> {
    return this.http.get<ChatRoomDTO[]>(`http://localhost:9090/api/support/my-tickets`);
  }

  // ✅ Récupérer tous les tickets pour l'admin
  getAdminSupportTickets(): Observable<ChatRoomDTO[]> {
    return this.http.get<ChatRoomDTO[]>(`http://localhost:9090/api/support/admin/tickets`);
  }

  // ✅ Dashboard admin avec stats
  getAdminSupportDashboard(): Observable<ChatRoomDTO[]> {
    return this.http.get<ChatRoomDTO[]>(`http://localhost:9090/api/support/admin/dashboard`);
  }

  // ✅ Résoudre un ticket (admin seulement)
  resolveSupportTicket(roomId: string): Observable<{message: string}> {
    return this.http.put<{message: string}>(`http://localhost:9090/api/support/admin/ticket/${roomId}/resolve`, {});
  }

  // ✅ Vérifier si l'utilisateur est admin
  isUserAdmin(): Observable<{isAdmin: boolean}> {
    return this.http.get<{isAdmin: boolean}>(`http://localhost:9090/api/support/is-admin`);
  }

  // ✅ Récupérer les types de support disponibles
  getSupportTypes(): Observable<{value: string, displayName: string}[]> {
    return this.http.get<{value: string, displayName: string}[]>(`http://localhost:9090/api/support/types`);
  }

  // ========== MÉTHODES UTILITAIRES ==========

  isImageFile(mimeType: string): boolean {
    return mimeType?.startsWith('image/') || false;
  }

  isVideoFile(mimeType: string): boolean {
    return mimeType?.startsWith('video/') || false;
  }

  isAudioFile(mimeType: string): boolean {
    return mimeType?.startsWith('audio/') || false;
  }

  getFileIcon(mimeType: string): string {
    if (this.isImageFile(mimeType)) return '🖼️';
    if (this.isVideoFile(mimeType)) return '🎥';
    if (this.isAudioFile(mimeType)) return '🎵';
    if (mimeType?.includes('pdf')) return '📄';
    if (mimeType?.includes('word')) return '📝';
    if (mimeType?.includes('excel') || mimeType?.includes('sheet')) return '📊';
    return '📎';
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

}