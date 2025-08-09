import {FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators} from '@angular/forms';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CommonModule, DatePipe } from '@angular/common';
import { Component, HostListener, OnDestroy, OnInit, ViewChild, ElementRef } from "@angular/core";
import { Observable, retry, Subject, Subscription } from "rxjs";
import { AuthService } from "../core/services/auth.service";
import { WebSocketService } from "./service/websocket.service";
import { ChatMessageDTO, ChatParticipantDTO, ChatRoomDTO, CreateGroupDTO, SendMessageDTO } from "./chat.models";
import { ChatService } from "./service/chat.service";
import { catchError } from "rxjs/operators";
import { trigger, state, style, transition, animate } from '@angular/animations';

// Define user interface for type safety
interface User {
  id: number;
  // Add other user properties as needed
}

// Interface pour les notifications toast
interface Toast {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  message: string;
  icon: string;
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
  styleUrl: './chat.component.scss',
  animations: [
    trigger('slideIn', [
      transition(':enter', [
        style({ opacity: 0, transform: 'translateX(100%)' }),
        animate('300ms ease-in', style({ opacity: 1, transform: 'translateX(0%)' }))
      ]),
      transition(':leave', [
        animate('300ms ease-out', style({ opacity: 0, transform: 'translateX(100%)' }))
      ])
    ])
  ]
})
export class ChatComponent implements OnInit, OnDestroy {
  @ViewChild('messageContainer', { static: false }) messageContainer!: ElementRef;
  @ViewChild('editTextarea', { static: false }) editTextarea!: ElementRef;

  chatRooms: ChatRoomDTO[] = [];
  selectedRoom: ChatRoomDTO | null = null;
  messages: ChatMessageDTO[] = [];
  participants: ChatParticipantDTO[] = [];
  newMessage = '';
  otherUserId = '';
  searchQuery = '';
  searchResults: ChatMessageDTO[] = [];
  groupName = '';
  groupDescription = '';
  participantIds = '';
  loading = false;
  sendingMessage = false;
  error = '';
  private subscriptions: Subscription[] = [];
  currentUserId: number | null = null;
  currentPage = 0;
  pageSize = 50;
  hasMoreMessages = true;
  private typingTimeout: any;
  private searchTimeout: any;
  isTyping = false;
  showMenuFor: number | null = null;
  replyingTo: ChatMessageDTO | null = null;
  showNewChatModal = false;
  newChatType: 'private' | 'group' = 'private';
  showEditModal = false;
  showDeleteConfirmModal = false;
  messageToEdit: ChatMessageDTO | null = null;
  messageToDelete: number | null = null;
  editedMessageContent = '';

  // Toast notifications
  toasts: Toast[] = [];
  private toastIdCounter = 0;

  constructor(
      private chatService: ChatService,
      private webSocketService: WebSocketService,
      private authService: AuthService
  ) { }

  ngOnInit(): void {
    this.getCurrentUserId();
    this.loadUserChats();
    this.setupWebSocket();
  }

  // ============ USER ID MANAGEMENT ============

  public async getCurrentUserId(): Promise<void> {
    try {
      console.log('🔍 === Getting Current User ID - DETAILED ===');

      if (!this.authService) {
        console.error('❌ AuthService is not available');
        this.currentUserId = null;
        return;
      }

      const isLoggedIn = this.authService.isLoggedIn();
      console.log('🔐 User logged in:', isLoggedIn);
      if (!isLoggedIn) {
        console.error('❌ User is not logged in');
        this.currentUserId = null;
        return;
      }

      const token = this.authService.getTokenSync();
      console.log('🎫 Token available:', !!token, token ? token.substring(0, 50) + '...' : 'NO TOKEN');

      if (token) {
        try {
          const payload = JSON.parse(atob(token.split('.')[1]));
          console.log('🔓 Token payload:', payload);
          console.log('🆔 Available IDs in token:', {
            sub: payload.sub,
            preferred_username: payload.preferred_username,
            userId: payload.userId,
            id: payload.id,
            user_id: payload.user_id,
            database_id: payload.database_id
          });
        } catch (tokenError) {
          console.error('❌ Error decoding token:', tokenError);
        }
      }

      console.log('🔄 Trying getUserId()...');
      const userId = await this.authService.getUserId();
      console.log('📋 getUserId() result:', userId, typeof userId);

      if (userId && userId > 0) {
        this.currentUserId = userId;
        console.log('✅ Successfully set currentUserId to:', this.currentUserId);
        return;
      }

      console.log('🔄 Trying getUserIdSync()...');
      const userIdSync = this.authService.getUserIdSync();
      console.log('📋 getUserIdSync() result:', userIdSync, typeof userIdSync);

      if (userIdSync && userIdSync > 0) {
        this.currentUserId = userIdSync;
        console.log('✅ Successfully set currentUserId from sync to:', this.currentUserId);
        return;
      }

      console.log('🔄 Trying getCurrentUser()...');
      const currentUser = this.authService.getCurrentUser();
      console.log('👤 getCurrentUser() result:', currentUser);

      if (currentUser && currentUser.id && currentUser.id > 0) {
        this.currentUserId = Number(currentUser.id);
        console.log('✅ Successfully set currentUserId from currentUser to:', this.currentUserId);
        return;
      }

      console.log('🔄 Trying localStorage fallback...');
      const localStorageKeys = ['userId', 'currentUserId', 'user_id', 'cachedUserId'];
      for (const key of localStorageKeys) {
        const value = localStorage.getItem(key);
        if (value && Number(value) > 0) {
          this.currentUserId = Number(value);
          console.log(`✅ Found user ID in localStorage[${key}]:`, this.currentUserId);
          return;
        }
      }

      console.warn('⚠️ All methods failed, using temporary fallback...');
      const tempId = prompt('DEBUG: Quel est votre ID utilisateur ? (temporaire pour tester)');
      if (tempId && Number(tempId) > 0) {
        this.currentUserId = Number(tempId);
        localStorage.setItem('tempUserId', tempId);
        console.log('🆘 Using temporary user ID:', this.currentUserId);
        return;
      }

      this.currentUserId = null;
      console.error('❌ ALL METHODS FAILED TO GET USER ID');

    } catch (error) {
      console.error('❌ Critical error getting current user ID:', error);
      this.currentUserId = null;
    }

    console.log('🏁 === Final currentUserId ===', this.currentUserId);
  }

  public async forceRefreshUserId(): Promise<void> {
    console.log('🔄 Forcing user ID refresh...');
    await this.getCurrentUserId();

    if (this.selectedRoom) {
      console.log('📱 Refreshing current room to update UI...');
      setTimeout(() => {
        console.log('UI should be updated now');
      }, 100);
    }
  }

  canEditMessage(message: ChatMessageDTO): boolean {
    console.log('🔍 Checking if can edit message:', {
      messageId: message.id,
      currentUserId: this.currentUserId,
      messageSenderId: message.senderId,
      currentUserIdType: typeof this.currentUserId,
      messageSenderIdType: typeof message.senderId
    });

    if (!this.currentUserId || this.currentUserId <= 0) {
      console.log('❌ Cannot edit: currentUserId is invalid');
      return false;
    }

    if (!message.senderId) {
      console.log('❌ Cannot edit: message.senderId is invalid');
      return false;
    }

    const currentUserId = Number(this.currentUserId);
    const messageSenderId = Number(message.senderId);

    console.log('🔢 Numeric comparison:', {
      currentUserId,
      messageSenderId,
      areEqual: currentUserId === messageSenderId
    });

    const canEdit = currentUserId === messageSenderId;
    console.log(canEdit ? '✅ Can edit message' : '❌ Cannot edit message');

    return canEdit;
  }

  public async debugUserInfo(): Promise<void> {
    console.log('🐛 === COMPLETE DEBUG INFO ===');
    await this.authService.debugUserInfo();
    console.log('Chat Component currentUserId:', this.currentUserId);
    console.log('Selected room:', this.selectedRoom?.name);

    if (this.messages.length > 0) {
      const firstMessage = this.messages[0];
      console.log('First message sender ID:', firstMessage.senderId);
      console.log('Can edit first message:', this.canEditMessage(firstMessage));
    }

    console.log('🐛 === END DEBUG ===');
  }

  // ============ TRACKING FUNCTIONS ============

  trackMessage(index: number, message: ChatMessageDTO): number {
    return message.id;
  }

  trackRoom(index: number, room: ChatRoomDTO): string | number {
    return room.roomId || room.id || index;
  }

  // ============ STATUS AND UTILITIES ============

  getStatusIcon(status?: string): string {
    switch (status) {
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

  getLastMessagePreview(room: ChatRoomDTO): string {
    return 'Aucun message'; // ou room.lastMessage si cette propriété existe
  }

  formatRelativeTime(dateString: string): string {
    const date = new Date(dateString);
    const now = new Date();
    const diff = now.getTime() - date.getTime();

    const seconds = Math.floor(diff / 1000);
    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(diff / 3600000);
    const days = Math.floor(diff / 86400000);
    const weeks = Math.floor(diff / 604800000);

    if (seconds < 30) return 'maintenant';
    if (minutes < 1) return 'il y a quelques instants';
    if (minutes < 60) return `${minutes}min`;
    if (hours < 24) return `${hours}h`;
    if (days < 7) return `${days}j`;
    if (weeks < 4) return `${weeks}sem`;

    return this.formatDate(dateString);
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    const now = new Date();

    if (date.toDateString() === now.toDateString()) {
      return date.toLocaleTimeString('fr-FR', {
        hour: '2-digit',
        minute: '2-digit'
      });
    }

    if (date.getFullYear() === now.getFullYear()) {
      return date.toLocaleDateString('fr-FR', {
        day: '2-digit',
        month: '2-digit'
      });
    }

    return date.toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: '2-digit'
    });
  }

  getInitials(name: string): string {
    if (!name || typeof name !== 'string') return '?';

    const cleanName = name.trim();
    if (!cleanName) return '?';

    const words = cleanName.split(' ').filter(word => word.length > 0);

    if (words.length === 0) return '?';
    if (words.length === 1) return words[0].charAt(0).toUpperCase();

    const first = words[0].charAt(0).toUpperCase();
    const last = words[words.length - 1].charAt(0).toUpperCase();

    return first + last;
  }

  getSenderName(senderId: number): string {
    if (senderId === this.currentUserId) return 'Vous';
    const participant = this.participants.find(p => p.userId === senderId);
    return participant?.fullName || participant?.username || 'Utilisateur';
  }

  clearError(): void {
    this.error = '';
  }

  // ============ WEBSOCKET SETUP ============

  private setupWebSocket(): void {
    this.webSocketService.connect();

    const messagesSub = this.webSocketService.getMessages().subscribe(message => {
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
        setTimeout(() => this.scrollToBottom(), 100);
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
        this.showToast('success', 'Chat privé créé avec succès', 'chat');
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
        this.showToast('success', 'Groupe créé avec succès', 'group');
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

    const content = this.newMessage.trim();
    if (!content) {
      this.error = 'Le message ne peut pas être vide';
      return;
    }

    if (this.sendingMessage) return;

    this.sendingMessage = true;

    const messageData: SendMessageDTO = {
      roomId: this.selectedRoom.roomId,
      content: content,
      type: 'TEXT',
      replyToId: this.replyingTo?.id
    };

    this.chatService.sendMessage(messageData).subscribe({
      next: (message) => {
        this.messages.push(message);
        this.sortMessagesByTimestamp();
        this.newMessage = '';
        this.error = '';
        this.sendingMessage = false;
        this.replyingTo = null;
        setTimeout(() => this.scrollToBottom(), 100);
      },
      error: (error) => {
        this.handleError(error, 'Envoi du message');
        this.sendingMessage = false;
      }
    });
  }

  private scrollToBottom(): void {
    try {
      setTimeout(() => {
        const messageContainer = document.querySelector('.messages-list');
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

  // ============ MESSAGE MANAGEMENT - MODALS INSTEAD OF ALERTS ============

  // Ouvrir le modal de modification
  openEditModal(message: ChatMessageDTO): void {
    if (!this.canEditMessage(message)) {
      this.showToast('error', 'Vous ne pouvez pas modifier ce message', 'error');
      return;
    }

    this.messageToEdit = message;
    this.editedMessageContent = message.content;
    this.showEditModal = true;

    // Focus sur le textarea après l'ouverture du modal
    setTimeout(() => {
      if (this.editTextarea?.nativeElement) {
        this.editTextarea.nativeElement.focus();
        this.editTextarea.nativeElement.select();
      }
    }, 100);
  }

  // Fermer le modal de modification
  closeEditModal(): void {
    this.showEditModal = false;
    this.messageToEdit = null;
    this.editedMessageContent = '';
  }

  // Sauvegarder le message modifié
  saveEditedMessage(): void {
    if (!this.messageToEdit || !this.editedMessageContent.trim()) {
      return;
    }

    const newContent = this.editedMessageContent.trim();
    if (newContent === this.messageToEdit.content.trim()) {
      this.closeEditModal();
      return;
    }

    const sub = this.chatService.editMessage(this.messageToEdit.id, newContent).subscribe({
      next: (updatedMessage) => {
        const index = this.messages.findIndex(m => m.id === this.messageToEdit!.id);
        if (index !== -1) {
          this.messages[index] = updatedMessage;
        }
        this.closeEditModal();
        this.showToast('success', 'Message modifié avec succès', 'edit');
        console.log('Message edited:', updatedMessage);
      },
      error: (error) => {
        this.handleError(error, 'Modification du message');
        this.showToast('error', 'Erreur lors de la modification', 'error');
      }
    });
    this.subscriptions.push(sub);
  }

  // Ouvrir le modal de confirmation de suppression
  openDeleteConfirmModal(messageId: number): void {
    const message = this.messages.find(m => m.id === messageId);
    if (!message || !this.canEditMessage(message)) {
      this.showToast('error', 'Vous ne pouvez pas supprimer ce message', 'error');
      return;
    }

    this.messageToDelete = messageId;
    this.showDeleteConfirmModal = true;
  }

  // Fermer le modal de confirmation de suppression
  closeDeleteConfirmModal(): void {
    this.showDeleteConfirmModal = false;
    this.messageToDelete = null;
  }

  // Confirmer la suppression du message
  confirmDeleteMessage(): void {
    if (!this.messageToDelete) return;

    const sub = this.chatService.deleteMessage(this.messageToDelete).subscribe({
      next: () => {
        this.messages = this.messages.filter(m => m.id !== this.messageToDelete);
        this.closeDeleteConfirmModal();
        this.showToast('success', 'Message supprimé avec succès', 'delete');
        console.log('Message deleted');
      },
      error: (error) => {
        this.handleError(error, 'Suppression du message');
        this.showToast('error', 'Erreur lors de la suppression', 'error');
      }
    });
    this.subscriptions.push(sub);
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

  // ============ REPLY FUNCTIONALITY ============

  replyToMessage(message: ChatMessageDTO): void {
    this.replyingTo = message;
    const input = document.querySelector('.message-input') as HTMLTextAreaElement;
    if (input) input.focus();
  }

  cancelReply(): void {
    this.replyingTo = null;
  }

  getRepliedMessageSender(replyToId: number): string {
    const repliedMessage = this.messages.find(m => m.id === replyToId);
    if (!repliedMessage) return 'message supprimé';
    return repliedMessage.senderId === this.currentUserId ? 'vous' : this.getSenderName(repliedMessage.senderId);
  }

  getRepliedMessageContent(replyToId: number): string {
    const repliedMessage = this.messages.find(m => m.id === replyToId);
    return repliedMessage?.content || 'Message supprimé';
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
            this.showToast('success', 'Participant ajouté avec succès', 'person_add');
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

    const sub = this.chatService.removeParticipant(this.selectedRoom.roomId, participantId).subscribe({
      next: () => {
        this.loadParticipants(this.selectedRoom!.roomId);
        this.showToast('success', 'Participant retiré avec succès', 'person_remove');
        console.log('Participant removed');
      },
      error: (error) => {
        this.handleError(error, 'Retrait du participant');
      }
    });
    this.subscriptions.push(sub);
  }

  leaveGroup(): void {
    if (!this.selectedRoom) return;

    const sub = this.chatService.leaveGroup(this.selectedRoom.roomId).subscribe({
      next: () => {
        this.loadUserChats();
        this.selectedRoom = null;
        this.messages = [];
        this.participants = [];
        this.showToast('info', 'Vous avez quitté le groupe', 'exit_to_app');
        console.log('Left group');
      },
      error: (error) => {
        this.handleError(error, 'Sortie du groupe');
      }
    });
    this.subscriptions.push(sub);
  }

  // ============ TOAST NOTIFICATIONS ============

  showToast(type: 'success' | 'error' | 'warning' | 'info', message: string, icon: string): void {
    const toast: Toast = {
      id: `toast-${this.toastIdCounter++}`,
      type,
      message,
      icon
    };

    this.toasts.push(toast);

    // Auto-remove after 5 seconds
    setTimeout(() => {
      this.removeToast(toast);
    }, 5000);
  }

  removeToast(toast: Toast): void {
    const index = this.toasts.findIndex(t => t.id === toast.id);
    if (index > -1) {
      this.toasts.splice(index, 1);
    }
  }

  // ============ KEYBOARD AND INPUT HANDLING ============

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

  // ============ STATE HELPERS ============

  isGroupAdmin(): boolean {
    if (!this.selectedRoom || this.selectedRoom.type !== 'GROUP' || !this.currentUserId) {
      return false;
    }
    return this.participants.some(p =>
        p.userId === this.currentUserId && p.role === 'ADMIN'
    );
  }

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

  private typingUsers = new Set<number>();

  handleTypingIndicator(data: { userId: number; roomId: string; isTyping: boolean }): void {
    if (data.userId === this.currentUserId) return;

    if (data.isTyping) {
      this.typingUsers.add(data.userId);
    } else {
      this.typingUsers.delete(data.userId);
    }
  }

  isSomeoneTyping(): boolean {
    return this.typingUsers.size > 0;
  }

  getTypingUserNames(): string {
    const names: string[] = [];
    this.typingUsers.forEach(userId => {
      const participant = this.participants.find(p => p.userId === userId);
      if (participant) {
        names.push(participant.fullName || participant.username || 'Utilisateur');
      }
    });

    if (names.length === 0) return '';
    if (names.length === 1) return `${names[0]} tape...`;
    if (names.length === 2) return `${names[0]} et ${names[1]} tapent...`;
    return `${names[0]} et ${names.length - 1} autres tapent...`;
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
      this.showToast('success', 'Conversation actualisée', 'refresh');
    }
  }

  copyInviteLink(): void {
    if (this.selectedRoom && this.selectedRoom.type === 'GROUP') {
      const inviteText = `Rejoignez notre groupe "${this.selectedRoom.name}" - Room ID: ${this.selectedRoom.roomId}`;
      if (navigator.clipboard) {
        navigator.clipboard.writeText(inviteText).then(() => {
          this.showToast('success', 'Lien d\'invitation copié', 'content_copy');
        }).catch(err => {
          console.error('Erreur lors de la copie:', err);
          this.showToast('error', 'Erreur lors de la copie', 'error');
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
      this.showToast('info', 'L\'envoi de fichiers n\'est pas encore implémenté', 'info');
    }
  }

  // ============ MODAL MANAGEMENT ============

  openNewChatModal(): void {
    this.showNewChatModal = true;
    this.newChatType = 'private';
    this.otherUserId = '';
    this.groupName = '';
    this.groupDescription = '';
    this.participantIds = '';
    this.error = '';
  }

  closeNewChatModal(): void {
    this.showNewChatModal = false;
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
    this.showToast('error', errorMessage, 'error');
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
          const searchInput = document.querySelector('.search-field') as HTMLInputElement;
          if (searchInput) {
            searchInput.focus();
          }
          break;
        case 'r':
          event.preventDefault();
          this.loadUserChats();
          break;
        case 'Escape':
          event.preventDefault();
          this.closeAllModals();
          break;
      }
    }
  }

  private closeAllModals(): void {
    this.showNewChatModal = false;
    this.showEditModal = false;
    this.showDeleteConfirmModal = false;
    this.replyingTo = null;
  }

  // ============ CLICK OUTSIDE HANDLING ============

  @HostListener('document:click', ['$event'])
  closeMenuOnClickOutside(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (!target.closest('.message-actions') && !target.closest('.action-btn')) {
      this.showMenuFor = null;
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


  isMessageDeleted(message: ChatMessageDTO): boolean {
    // Vous pouvez implémenter votre logique ici
    // Par exemple, vérifier si le contenu est vide ou contient un texte spécifique
    return message.content === '[DELETED]' ||
        message.content === 'Ce message a été supprimé' ||
        message.content === 'Vous avez supprimé ce message';
  }
}