import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import {BehaviorSubject, Observable, Subject} from 'rxjs';
import { environment } from '../../../environments/environment';
import {ChatMessageDTO, ChatParticipantDTO, ChatRoomDTO, CreateGroupDTO, SendMessageDTO} from "../chat.models";


@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private baseUrl = 'http://localhost:9090/fantasy/api/chat';

  constructor(private http: HttpClient) { }

  // Get user chats
  getUserChats(): Observable<ChatRoomDTO[]> {
    return this.http.get<ChatRoomDTO[]>(`${this.baseUrl}/rooms`);
  }

  // Create or get private chat
  createOrGetPrivateChat(otherUserId: number): Observable<ChatRoomDTO> {
    return this.http.post<ChatRoomDTO>(`${this.baseUrl}/rooms/private/${otherUserId}`, {});
  }

  // Create group chat
  createGroup(groupData: CreateGroupDTO): Observable<ChatRoomDTO> {
    return this.http.post<ChatRoomDTO>(`${this.baseUrl}/rooms/group`, groupData);
  }

  // Send message
  sendMessage(messageData: SendMessageDTO): Observable<ChatMessageDTO> {
    return this.http.post<ChatMessageDTO>(`${this.baseUrl}/messages`, messageData);
  }

  // Get room messages
  getRoomMessages(roomId: string, page: number = 0, size: number = 50): Observable<any> {
    const params = new HttpParams()
        .set('page', page.toString())
        .set('size', size.toString());

    return this.http.get<any>(`${this.baseUrl}/rooms/${roomId}/messages`, { params });
  }

  // Mark message as read
  markAsRead(messageId: number, roomId: string): Observable<void> {
    const params = new HttpParams().set('roomId', roomId);
    return this.http.post<void>(`${this.baseUrl}/messages/${messageId}/read`, {}, { params });
  }

  // Search messages
  searchMessages(roomId: string, query: string): Observable<ChatMessageDTO[]> {
    const params = new HttpParams().set('query', query);
    return this.http.get<ChatMessageDTO[]>(`${this.baseUrl}/rooms/${roomId}/search`, { params });
  }

  // Edit message
  editMessage(messageId: number, newContent: string): Observable<ChatMessageDTO> {
    return this.http.put<ChatMessageDTO>(`${this.baseUrl}/messages/${messageId}`, newContent, {
      headers: {
        'Content-Type': 'text/plain'
      }
    });
  }

  // Delete message
  deleteMessage(messageId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/messages/${messageId}`);
  }

  // Get active participants
  getActiveParticipants(roomId: string): Observable<ChatParticipantDTO[]> {
    return this.http.get<ChatParticipantDTO[]>(`${this.baseUrl}/rooms/${roomId}/participants`);
  }

  // Add participants to group
  addParticipants(roomId: string, userIds: number[]): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/rooms/${roomId}/participants`, userIds);
  }

  // Remove participant
  removeParticipant(roomId: string, participantId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/rooms/${roomId}/participants/${participantId}`);
  }

  // Leave group
  leaveGroup(roomId: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/rooms/${roomId}/leave`, {});
  }

  // Update group info
  updateGroupInfo(roomId: string, updateData: CreateGroupDTO): Observable<ChatRoomDTO> {
    return this.http.put<ChatRoomDTO>(`${this.baseUrl}/rooms/${roomId}`, updateData);
  }
}