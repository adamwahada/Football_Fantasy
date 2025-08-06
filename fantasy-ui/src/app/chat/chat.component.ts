import {FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators} from '@angular/forms';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CommonModule, DatePipe } from '@angular/common';
import {Component, HostListener, OnDestroy, OnInit} from "@angular/core";

import {Observable, retry, Subject, Subscription} from "rxjs";
import {AuthService} from "../core/services/auth.service";
import {WebSocketService} from "./service/websocket.service";
import {ChatMessageDTO, ChatParticipantDTO, ChatRoomDTO, CreateGroupDTO, SendMessageDTO} from "./chat.models";
import {ChatService} from "./service/chat.service";
import {catchError} from "rxjs/operators";

// Define user interface for type safety
interface User {
  id: number;
  // Add other user properties as needed
}

@Component({
  selector: 'app-chat',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatMenuModule,
    MatCardModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    FormsModule,
  ],
  templateUrl: './chat.component.html',
  standalone: true,
  styleUrl: './chat.component.scss'
})

export class ChatComponent implements OnInit, OnDestroy {

  // État principal
  chatRooms: ChatRoomDTO[] = [];
  selectedRoom: ChatRoomDTO | null = null;
  messages: ChatMessageDTO[] = [];
  participants: ChatParticipantDTO[] = [];

  // Formulaires
  newMessage = '';
  otherUserId = '';
  searchQuery = '';
  searchResults: ChatMessageDTO[] = [];

  // Groupes
  groupName = '';
  groupDescription = '';
  participantIds = '';

  // États
  loading = false;
  sendingMessage = false;
  error = '';

  // Subscriptions
  private subscriptions: Subscription[] = [];

  currentUserId: number | null = null;

  // Pagination
  currentPage = 0;
  pageSize = 50;
  hasMoreMessages = true;

  // Typing status
  private typingTimeout: any;
  private searchTimeout: any;
  isTyping = false;

  constructor(
      private chatService: ChatService,
      private webSocketService: WebSocketService,
      private authService: AuthService
  ) { }

  ngOnInit(): void {
    this.loadUserChats();
    this.setupWebSocket();
    this.getCurrentUserId();
  }

  private getCurrentUserId(): void {
    try {
      const userId = this.authService.getUserId();
      // getUserId() returns 0 if no token or invalid token, so treat 0 as null
      this.currentUserId = userId > 0 ? userId : null;
      console.log('Current user ID:', this.currentUserId);
    } catch (error) {
      console.error('Error getting current user ID:', error);
      this.currentUserId = null;
    }
  }

  trackMessage(index: number, message: ChatMessageDTO): number {
    return message.id;
  }

  getStatusIcon(status: string): string {
    switch (status?.toLowerCase()) {
      case 'sent':
        return 'check';
      case 'delivered':
        return 'done_all';
      case 'read':
        return 'done_all';
      default:
        return 'schedule';
    }
  }

  private setupWebSocket(): void {
    this.webSocketService.connect();

    // Listen for new messages
    const messagesSub = this.webSocketService.getMessages().subscribe(message => {
      // Fix: Check if message has roomId property, or use alternative property
      const messageRoomId = (message as any).roomId || (message as any).chatRoomId;

      if (message && this.selectedRoom && messageRoomId === this.selectedRoom.roomId) {
        const exists = this.messages.find(m => m.id === message.id);
        if (!exists) {
          this.messages.push(message);
          this.sortMessagesByTimestamp();
        }
        this.loadUserChats();
      }
    });

    // Listen for connection status
    const connectionSub = this.webSocketService.isConnected().subscribe(connected => {
      console.log('WebSocket connection status:', connected);
    });

    this.subscriptions.push(messagesSub, connectionSub);
  }

  private sortMessagesByTimestamp(): void {
    this.messages.sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime());
  }

  // ============ DATA LOADING ============

  loadUserChats(): void {
    this.loading = true;
    const sub = this.chatService.getUserChats().subscribe({
      next: (rooms) => {
        this.chatRooms = rooms;
        this.loading = false;
        console.log('Chats loaded:', rooms);
      },
      error: (error) => {
        this.handleError(error, 'Chargement des chats');
        this.loading = false;
      }
    });
    this.subscriptions.push(sub);
  }

  selectRoom(room: ChatRoomDTO): void {
    this.selectedRoom = room;
    this.messages = [];
    this.participants = [];
    this.searchResults = [];
    this.searchQuery = '';
    this.resetPagination();

    this.loadRoomMessages(room.roomId);
    this.loadParticipants(room.roomId);
    this.markRoomAsViewed(room.roomId);
  }

  loadRoomMessages(roomId: string): void {
    const sub = this.chatService.getRoomMessages(roomId, this.currentPage, this.pageSize).subscribe({
      next: (response) => {
        this.messages = response.content || [];
        this.sortMessagesByTimestamp();
        console.log('Messages loaded:', this.messages);
      },
      error: (error) => {
        this.handleError(error, 'Chargement des messages');
      }
    });
    this.subscriptions.push(sub);
  }

  loadParticipants(roomId: string): void {
    const sub = this.chatService.getActiveParticipants(roomId).subscribe({
      next: (participants) => {
        this.participants = participants;
        console.log('Participants loaded:', participants);
      },
      error: (error) => {
        console.error('Error loading participants:', error);
      }
    });
    this.subscriptions.push(sub);
  }

  private markRoomAsViewed(roomId: string): void {
    // Implementation for marking room as viewed
  }

  // ============ CHAT CREATION ============

  createPrivateChat(): void {
    const validationError = this.validatePrivateChatForm();
    if (validationError) {
      this.error = validationError;
      return;
    }

    const userId = parseInt(this.otherUserId);
    const sub = this.chatService.createOrGetPrivateChat(userId).subscribe({
      next: (room) => {
        console.log('Private chat created/found:', room);
        this.loadUserChats();
        this.otherUserId = '';
        this.error = '';
        this.selectRoom(room);
      },
      error: (error) => {
        this.handleError(error, 'Création du chat privé');
      }
    });
    this.subscriptions.push(sub);
  }

  createGroup(): void {
    const validationError = this.validateGroupForm();
    if (validationError) {
      this.error = validationError;
      return;
    }

    const participantIdsList = this.participantIds
        .split(',')
        .map(id => parseInt(id.trim()))
        .filter(id => !isNaN(id));

    const groupData: CreateGroupDTO = {
      name: this.groupName.trim(),
      description: this.groupDescription.trim(),
      participantIds: participantIdsList
    };

    const sub = this.chatService.createGroup(groupData).subscribe({
      next: (room) => {
        console.log('Group created:', room);
        this.loadUserChats();
        this.groupName = '';
        this.groupDescription = '';
        this.participantIds = '';
        this.error = '';
        this.selectRoom(room);
      },
      error: (error) => {
        this.handleError(error, 'Création du groupe');
      }
    });
    this.subscriptions.push(sub);
  }

  // ============ MESSAGE SENDING ============

  sendMessage(): void {
    if (!this.selectedRoom) {
      this.error = 'Aucun chat sélectionné';
      return;
    }

    if (!this.newMessage.trim()) {
      this.error = 'Le message ne peut pas être vide';
      return;
    }

    if (this.sendingMessage) {
      return;
    }

    this.sendingMessage = true;

    const messageData: SendMessageDTO = {
      roomId: this.selectedRoom.roomId,
      content: this.newMessage.trim(),
      type: 'TEXT'
    };

    const sub = this.chatService.sendMessage(messageData).subscribe({
      next: (message) => {
        console.log('Message sent:', message);

        const exists = this.messages.find(m => m.id === message.id);
        if (!exists) {
          this.messages.push(message);
          this.sortMessagesByTimestamp();
        }

        this.newMessage = '';
        this.error = '';
        this.sendingMessage = false;

        setTimeout(() => this.scrollToBottom(), 100);
      },
      error: (error) => {
        this.handleError(error, 'Envoi du message');
        this.sendingMessage = false;
      }
    });
    this.subscriptions.push(sub);
  }

  private scrollToBottom(): void {
    try {
      setTimeout(() => {
        const messageContainer = document.querySelector('.message-list');
        if (messageContainer) {
          messageContainer.scrollTo({
            top: messageContainer.scrollHeight,
            behavior: 'smooth'
          });
        }
      }, 100);
    } catch (err) {
      console.error('Error scrolling to bottom:', err);
    }
  }

  // ============ SEARCH ============

  searchMessages(): void {
    if (!this.selectedRoom || !this.searchQuery.trim()) {
      return;
    }

    const sub = this.chatService.searchMessages(this.selectedRoom.roomId, this.searchQuery).subscribe({
      next: (results) => {
        this.searchResults = results;
        console.log('Search results:', results);
      },
      error: (error) => {
        this.handleError(error, 'Recherche');
      }
    });
    this.subscriptions.push(sub);
  }

  clearSearch(): void {
    this.searchQuery = '';
    this.searchResults = [];
  }

  onSearchInput(): void {
    if (this.searchTimeout) {
      clearTimeout(this.searchTimeout);
    }

    this.searchTimeout = setTimeout(() => {
      if (this.searchQuery.trim()) {
        this.searchMessages();
      } else {
        this.clearSearch();
      }
    }, 300);
  }

  // ============ MESSAGE MANAGEMENT ============

  editMessage(messageId: number, currentContent: string): void {
    const newContent = prompt('Nouveau contenu:', currentContent);
    if (newContent && newContent !== currentContent) {
      const sub = this.chatService.editMessage(messageId, newContent).subscribe({
        next: (updatedMessage) => {
          const index = this.messages.findIndex(m => m.id === messageId);
          if (index !== -1) {
            this.messages[index] = updatedMessage;
          }
          console.log('Message edited:', updatedMessage);
        },
        error: (error) => {
          this.handleError(error, 'Modification du message');
        }
      });
      this.subscriptions.push(sub);
    }
  }

  deleteMessage(messageId: number): void {
    if (confirm('Voulez-vous vraiment supprimer ce message ?')) {
      const sub = this.chatService.deleteMessage(messageId).subscribe({
        next: () => {
          this.messages = this.messages.filter(m => m.id !== messageId);
          console.log('Message deleted');
        },
        error: (error) => {
          this.handleError(error, 'Suppression du message');
        }
      });
      this.subscriptions.push(sub);
    }
  }

  markAsRead(messageId: number): void {
    if (!this.selectedRoom) return;

    const sub = this.chatService.markAsRead(messageId, this.selectedRoom.roomId).subscribe({
      next: () => {
        const message = this.messages.find(m => m.id === messageId);
        if (message) {
          message.status = 'READ';
        }
        console.log('Message marked as read');
      },
      error: (error) => {
        console.error('Error marking message as read:', error);
      }
    });
    this.subscriptions.push(sub);
  }

  // ============ GROUP MANAGEMENT ============

  addParticipant(): void {
    if (!this.selectedRoom) return;

    const userIdStr = prompt('ID de l\'utilisateur à ajouter:');
    if (userIdStr) {
      const userId = parseInt(userIdStr);
      if (!isNaN(userId)) {
        const sub = this.chatService.addParticipants(this.selectedRoom.roomId, [userId]).subscribe({
          next: () => {
            this.loadParticipants(this.selectedRoom!.roomId);
            console.log('Participant added');
          },
          error: (error) => {
            this.handleError(error, 'Ajout du participant');
          }
        });
        this.subscriptions.push(sub);
      }
    }
  }

  removeParticipant(participantId: number): void {
    if (!this.selectedRoom) return;

    if (confirm('Voulez-vous vraiment retirer ce participant ?')) {
      const sub = this.chatService.removeParticipant(this.selectedRoom.roomId, participantId).subscribe({
        next: () => {
          this.loadParticipants(this.selectedRoom!.roomId);
          console.log('Participant removed');
        },
        error: (error) => {
          this.handleError(error, 'Retrait du participant');
        }
      });
      this.subscriptions.push(sub);
    }
  }

  leaveGroup(): void {
    if (!this.selectedRoom) return;

    if (confirm('Voulez-vous vraiment quitter ce groupe ?')) {
      const sub = this.chatService.leaveGroup(this.selectedRoom.roomId).subscribe({
        next: () => {
          this.loadUserChats();
          this.selectedRoom = null;
          this.messages = [];
          this.participants = [];
          console.log('Left group');
        },
        error: (error) => {
          this.handleError(error, 'Sortie du groupe');
        }
      });
      this.subscriptions.push(sub);
    }
  }

  // ============ UTILITIES ============

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleString();
  }

  formatRelativeTime(dateString: string): string {
    const date = new Date(dateString);
    const now = new Date();
    const diff = now.getTime() - date.getTime();

    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(diff / 3600000);
    const days = Math.floor(diff / 86400000);

    if (minutes < 1) return 'À l\'instant';
    if (minutes < 60) return `Il y a ${minutes} min`;
    if (hours < 24) return `Il y a ${hours}h`;
    if (days < 7) return `Il y a ${days}j`;

    return this.formatDate(dateString);
  }

  clearError(): void {
    this.error = '';
  }

  onKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Enter') {
      if (event.ctrlKey || event.metaKey) {
        event.preventDefault();
        this.sendMessage();
      } else if (!event.shiftKey) {
        event.preventDefault();
        this.sendMessage();
      }
    }
  }

  canEditMessage(message: ChatMessageDTO): boolean {
    return this.currentUserId !== null && message.senderId === this.currentUserId;
  }

  isGroupAdmin(): boolean {
    if (!this.selectedRoom || this.selectedRoom.type !== 'GROUP' || !this.currentUserId) {
      return false;
    }

    return this.participants.some(p =>
        p.userId === this.currentUserId && p.role === 'ADMIN'
    );
  }

  // ============ VALIDATION ============

  private validatePrivateChatForm(): string | null {
    if (!this.otherUserId.trim()) {
      return 'Veuillez saisir un ID utilisateur';
    }

    const userId = parseInt(this.otherUserId);
    if (isNaN(userId) || userId <= 0) {
      return 'ID utilisateur invalide';
    }

    if (this.currentUserId && userId === this.currentUserId) {
      return 'Vous ne pouvez pas créer un chat avec vous-même';
    }

    return null;
  }

  private validateGroupForm(): string | null {
    if (!this.groupName.trim()) {
      return 'Veuillez saisir un nom de groupe';
    }

    if (this.groupName.trim().length < 2) {
      return 'Le nom du groupe doit contenir au moins 2 caractères';
    }

    const participantIdsList = this.participantIds
        .split(',')
        .map(id => parseInt(id.trim()))
        .filter(id => !isNaN(id));

    if (participantIdsList.length === 0) {
      return 'Veuillez ajouter au moins un participant';
    }

    return null;
  }

  // ============ PAGINATION ============

  loadMoreMessages(): void {
    if (!this.selectedRoom || !this.hasMoreMessages) return;

    this.currentPage++;

    const sub = this.chatService.getRoomMessages(this.selectedRoom.roomId, this.currentPage, this.pageSize).subscribe({
      next: (response) => {
        const newMessages = response.content || [];

        if (newMessages.length === 0) {
          this.hasMoreMessages = false;
          return;
        }

        this.messages = [...newMessages, ...this.messages];
        this.sortMessagesByTimestamp();

        console.log(`Loaded ${newMessages.length} more messages`);
      },
      error: (error) => {
        console.error('Error loading more messages:', error);
        this.currentPage--;
      }
    });

    this.subscriptions.push(sub);
  }

  private resetPagination(): void {
    this.currentPage = 0;
    this.hasMoreMessages = true;
  }

  onScroll(event: Event): void {
    const element = event.target as HTMLElement;

    if (element.scrollTop < 100 && this.hasMoreMessages) {
      this.loadMoreMessages();
    }
  }

  // ============ ADDITIONAL FEATURES ============

  refreshCurrentRoom(): void {
    if (this.selectedRoom) {
      this.loadRoomMessages(this.selectedRoom.roomId);
      this.loadParticipants(this.selectedRoom.roomId);
    }
  }

  copyInviteLink(): void {
    if (this.selectedRoom && this.selectedRoom.type === 'GROUP') {
      const inviteText = `Rejoignez notre groupe "${this.selectedRoom.name}" - Room ID: ${this.selectedRoom.roomId}`;

      if (navigator.clipboard) {
        navigator.clipboard.writeText(inviteText).then(() => {
          console.log('Lien d\'invitation copié');
        }).catch(err => {
          console.error('Erreur lors de la copie:', err);
        });
      }
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];

      const maxSize = 10 * 1024 * 1024; // 10MB
      if (file.size > maxSize) {
        this.error = 'Le fichier est trop volumineux (max 10MB)';
        return;
      }

      console.log('Fichier sélectionné:', file.name);
      this.error = 'L\'envoi de fichiers n\'est pas encore implémenté';
    }
  }

  // ============ STATE HELPERS ============

  isRoomSelected(): boolean {
    return this.selectedRoom !== null;
  }

  isGroupRoom(): boolean {
    return this.selectedRoom?.type === 'GROUP';
  }

  isPrivateRoom(): boolean {
    return this.selectedRoom?.type === 'PRIVATE';
  }

  // ============ TYPING INDICATOR ============

  onTyping(): void {
    if (!this.isTyping && this.selectedRoom) {
      this.isTyping = true;
    }

    if (this.typingTimeout) {
      clearTimeout(this.typingTimeout);
    }

    this.typingTimeout = setTimeout(() => {
      this.isTyping = false;
    }, 2000);
  }

  // ============ ERROR HANDLING ============

  private handleError(error: any, context: string): void {
    console.error(`Error in ${context}:`, error);

    let errorMessage = 'Une erreur inattendue s\'est produite';

    if (error.error?.message) {
      errorMessage = error.error.message;
    } else if (error.message) {
      errorMessage = error.message;
    } else if (typeof error === 'string') {
      errorMessage = error;
    }

    this.error = `${context}: ${errorMessage}`;
  }

  private retryOperation<T>(
      operation: () => Observable<T>,
      maxRetries: number = 3,
      delay: number = 1000
  ): Observable<T> {
    return operation().pipe(
        retry({
          count: maxRetries,
          delay: delay
        }),
        catchError(error => {
          console.error(`Operation failed after ${maxRetries} retries:`, error);
          throw error;
        })
    );
  }

  // ============ KEYBOARD SHORTCUTS ============

  @HostListener('window:keydown', ['$event'])
  onKeyboardShortcut(event: KeyboardEvent): void {
    if (event.ctrlKey || event.metaKey) {
      switch (event.key) {
        case 'k':
          event.preventDefault();
          const searchInput = document.querySelector('.search-field input') as HTMLInputElement;
          if (searchInput) {
            searchInput.focus();
          }
          break;
        case 'r':
          event.preventDefault();
          this.loadUserChats();
          break;
      }
    }
  }

  // ============ CLEANUP ============

  ngOnDestroy(): void {
    if (this.searchTimeout) {
      clearTimeout(this.searchTimeout);
    }

    if (this.typingTimeout) {
      clearTimeout(this.typingTimeout);
    }

    this.subscriptions.forEach(sub => {
      if (sub && !sub.closed) {
        sub.unsubscribe();
      }
    });

    this.webSocketService.disconnect();

    console.log('ChatComponent destroyed and cleaned up');
  }
  getInitials(name: string): string {
    if (!name) return '';
    const parts = name.split(' ');
    let initials = parts[0].charAt(0).toUpperCase();
    if (parts.length > 1) {
      initials += parts[parts.length - 1].charAt(0).toUpperCase();
    }
    return initials;
  }





}