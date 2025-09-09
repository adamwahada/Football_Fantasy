import { Injectable } from '@angular/core';
import * as SockJS from 'sockjs-client';
import * as Stomp from 'stompjs';
import { Observable, Subject, BehaviorSubject } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import { environment } from '../../../environments/environment';
import {Client} from "stompjs";
import {StompSubscription} from "@stomp/stompjs";
import {ChatMessageDTO, UserStatusDTO} from "../models/chat.models";
import {webSocket, WebSocketSubject} from "rxjs/webSocket";

@Injectable({
  providedIn: 'root'
})
@Injectable({
  providedIn: 'root'
})
export class WebSocketService {
  private connected = new BehaviorSubject<boolean>(false);
  private messageSubject = new BehaviorSubject<ChatMessageDTO | null>(null);
  private userStatusSubject = new BehaviorSubject<UserStatusDTO | null>(null);

  constructor() { }

  // Simulation de connexion (pour les tests sans WebSocket)
  connect(): void {
    console.log('WebSocket service initialized (simulation mode)');
    this.connected.next(true);
  }

  disconnect(): void {
    console.log('WebSocket disconnected (simulation mode)');
    this.connected.next(false);
  }

  isConnected(): Observable<boolean> {
    return this.connected.asObservable();
  }

  getMessages(): Observable<ChatMessageDTO | null> {
    return this.messageSubject.asObservable();
  }

  getUserStatus(): Observable<UserStatusDTO | null> {
    return this.userStatusSubject.asObservable();
  }

  // Méthode pour simuler la réception d'un message (pour tests)
  simulateMessage(message: ChatMessageDTO): void {
    this.messageSubject.next(message);
  }

  // Méthode pour simuler un changement de statut utilisateur
  simulateUserStatus(status: UserStatusDTO): void {
    this.userStatusSubject.next(status);
  }
}