import {FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators} from '@angular/forms';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CommonModule, DatePipe } from '@angular/common';
import {Component, OnDestroy, OnInit} from "@angular/core";

import {Subject, Subscription} from "rxjs";
import {AuthService} from "../core/services/auth.service";
import {WebSocketService} from "./service/websocket.service";
import {ChatMessageDTO, ChatParticipantDTO, ChatRoomDTO, CreateGroupDTO, SendMessageDTO} from "./chat.models";
import {ChatService} from "./service/chat.service";


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
  error = '';

  constructor(
      private chatService: ChatService,
      private webSocketService: WebSocketService
  ) { }

  ngOnInit(): void {
    this.loadUserChats();

    // Connexion WebSocket
    this.webSocketService.connect();

    // Écouter les nouveaux messages
    this.webSocketService.getMessages().subscribe(message => {
      if (message && this.selectedRoom) {
        this.messages.push(message);
        this.loadUserChats(); // Refresh chat list for unread count
      }
    });

    // Écouter l'état de connexion
    this.webSocketService.isConnected().subscribe(connected => {
      console.log('WebSocket connection status:', connected);
    });
  }

  ngOnDestroy(): void {
    this.webSocketService.disconnect();
  }

  // ============ CHARGEMENT DES DONNÉES ============

  loadUserChats(): void {
    this.loading = true;
    this.chatService.getUserChats().subscribe({
      next: (rooms) => {
        this.chatRooms = rooms;
        this.loading = false;
        console.log('Chats loaded:', rooms);
      },
      error: (error) => {
        this.error = 'Erreur lors du chargement des chats: ' + (error.error?.message || error.message);
        this.loading = false;
        console.error('Error loading chats:', error);
      }
    });
  }

  selectRoom(room: ChatRoomDTO): void {
    this.selectedRoom = room;
    this.messages = [];
    this.participants = [];
    this.searchResults = [];
    this.searchQuery = '';
    this.loadRoomMessages(room.roomId);
    this.loadParticipants(room.roomId);
  }

  loadRoomMessages(roomId: string): void {
    this.chatService.getRoomMessages(roomId).subscribe({
      next: (response) => {
        this.messages = response.content || [];
        console.log('Messages loaded:', this.messages);
      },
      error: (error) => {
        this.error = 'Erreur lors du chargement des messages: ' + (error.error?.message || error.message);
        console.error('Error loading messages:', error);
      }
    });
  }

  loadParticipants(roomId: string): void {
    this.chatService.getActiveParticipants(roomId).subscribe({
      next: (participants) => {
        this.participants = participants;
        console.log('Participants loaded:', participants);
      },
      error: (error) => {
        console.error('Error loading participants:', error);
      }
    });
  }

  // ============ CRÉATION DE CHATS ============

  createPrivateChat(): void {
    if (!this.otherUserId.trim()) {
      this.error = 'Veuillez saisir un ID utilisateur';
      return;
    }

    const userId = parseInt(this.otherUserId);
    if (isNaN(userId)) {
      this.error = 'ID utilisateur invalide';
      return;
    }

    this.chatService.createOrGetPrivateChat(userId).subscribe({
      next: (room) => {
        console.log('Private chat created/found:', room);
        this.loadUserChats();
        this.otherUserId = '';
        this.error = '';
      },
      error: (error) => {
        this.error = 'Erreur lors de la création du chat privé: ' + (error.error?.message || error.message);
        console.error('Error creating private chat:', error);
      }
    });
  }

  createGroup(): void {
    if (!this.groupName.trim()) {
      this.error = 'Veuillez saisir un nom de groupe';
      return;
    }

    const participantIdsList = this.participantIds
        .split(',')
        .map(id => parseInt(id.trim()))
        .filter(id => !isNaN(id));

    const groupData: CreateGroupDTO = {
      name: this.groupName,
      description: this.groupDescription,
      participantIds: participantIdsList
    };

    this.chatService.createGroup(groupData).subscribe({
      next: (room) => {
        console.log('Group created:', room);
        this.loadUserChats();
        this.groupName = '';
        this.groupDescription = '';
        this.participantIds = '';
        this.error = '';
      },
      error: (error) => {
        this.error = 'Erreur lors de la création du groupe: ' + (error.error?.message || error.message);
        console.error('Error creating group:', error);
      }
    });
  }

  // ============ ENVOI DE MESSAGES ============

  sendMessage(): void {
    if (!this.selectedRoom || !this.newMessage.trim()) {
      return;
    }

    const messageData: SendMessageDTO = {
      roomId: this.selectedRoom.roomId,
      content: this.newMessage,
      type: 'TEXT'
    };

    this.chatService.sendMessage(messageData).subscribe({
      next: (message) => {
        console.log('Message sent:', message);
        this.messages.push(message);
        this.newMessage = '';
        this.error = '';
      },
      error: (error) => {
        this.error = 'Erreur lors de l\'envoi du message: ' + (error.error?.message || error.message);
        console.error('Error sending message:', error);
      }
    });
  }

  // ============ RECHERCHE ============

  searchMessages(): void {
    if (!this.selectedRoom || !this.searchQuery.trim()) {
      return;
    }

    this.chatService.searchMessages(this.selectedRoom.roomId, this.searchQuery).subscribe({
      next: (results) => {
        this.searchResults = results;
        console.log('Search results:', results);
      },
      error: (error) => {
        this.error = 'Erreur lors de la recherche: ' + (error.error?.message || error.message);
        console.error('Error searching messages:', error);
      }
    });
  }

  clearSearch(): void {
    this.searchQuery = '';
    this.searchResults = [];
  }

  // ============ GESTION DES MESSAGES ============

  editMessage(messageId: number, currentContent: string): void {
    const newContent = prompt('Nouveau contenu:', currentContent);
    if (newContent && newContent !== currentContent) {
      this.chatService.editMessage(messageId, newContent).subscribe({
        next: (updatedMessage) => {
          const index = this.messages.findIndex(m => m.id === messageId);
          if (index !== -1) {
            this.messages[index] = updatedMessage;
          }
          console.log('Message edited:', updatedMessage);
        },
        error: (error) => {
          this.error = 'Erreur lors de la modification du message: ' + (error.error?.message || error.message);
          console.error('Error editing message:', error);
        }
      });
    }
  }

  deleteMessage(messageId: number): void {
    if (confirm('Voulez-vous vraiment supprimer ce message ?')) {
      this.chatService.deleteMessage(messageId).subscribe({
        next: () => {
          this.loadRoomMessages(this.selectedRoom!.roomId);
          console.log('Message deleted');
        },
        error: (error) => {
          this.error = 'Erreur lors de la suppression du message: ' + (error.error?.message || error.message);
          console.error('Error deleting message:', error);
        }
      });
    }
  }

  markAsRead(messageId: number): void {
    if (!this.selectedRoom) return;

    this.chatService.markAsRead(messageId, this.selectedRoom.roomId).subscribe({
      next: () => {
        console.log('Message marked as read');
      },
      error: (error) => {
        console.error('Error marking message as read:', error);
      }
    });
  }

  // ============ GESTION DES GROUPES ============

  addParticipant(): void {
    if (!this.selectedRoom) return;

    const userIdStr = prompt('ID de l\'utilisateur à ajouter:');
    if (userIdStr) {
      const userId = parseInt(userIdStr);
      if (!isNaN(userId)) {
        this.chatService.addParticipants(this.selectedRoom.roomId, [userId]).subscribe({
          next: () => {
            this.loadParticipants(this.selectedRoom!.roomId);
            console.log('Participant added');
          },
          error: (error) => {
            this.error = 'Erreur lors de l\'ajout du participant: ' + (error.error?.message || error.message);
            console.error('Error adding participant:', error);
          }
        });
      }
    }
  }

  removeParticipant(participantId: number): void {
    if (!this.selectedRoom) return;

    if (confirm('Voulez-vous vraiment retirer ce participant ?')) {
      this.chatService.removeParticipant(this.selectedRoom.roomId, participantId).subscribe({
        next: () => {
          this.loadParticipants(this.selectedRoom!.roomId);
          console.log('Participant removed');
        },
        error: (error) => {
          this.error = 'Erreur lors du retrait du participant: ' + (error.error?.message || error.message);
          console.error('Error removing participant:', error);
        }
      });
    }
  }

  leaveGroup(): void {
    if (!this.selectedRoom) return;

    if (confirm('Voulez-vous vraiment quitter ce groupe ?')) {
      this.chatService.leaveGroup(this.selectedRoom.roomId).subscribe({
        next: () => {
          this.loadUserChats();
          this.selectedRoom = null;
          this.messages = [];
          this.participants = [];
          console.log('Left group');
        },
        error: (error) => {
          this.error = 'Erreur lors de la sortie du groupe: ' + (error.error?.message || error.message);
          console.error('Error leaving group:', error);
        }
      });
    }
  }

  // ============ UTILITAIRES ============

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleString();
  }

  clearError(): void {
    this.error = '';
  }

  // Gestion du keydown avec type correct
  onKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && event.ctrlKey) {
      this.sendMessage();
    }
  }
}