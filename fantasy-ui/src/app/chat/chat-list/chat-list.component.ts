import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { ChatRoomDTO } from "../chat.models";
import { ChatService } from "../service/chat.service";
import { DatePipe, NgForOf, NgIf } from "@angular/common";

@Component({
  selector: 'app-chat-list',
  imports: [
    NgForOf,
    NgIf,
    DatePipe
  ],
  templateUrl: './chat-list.component.html',
  styleUrl: './chat-list.component.scss',
  standalone: true,
})
export class ChatListComponent implements OnInit {
  chats: ChatRoomDTO[] = [];
  loading = false;
  selectedRoomId?: string;

  @Output() roomSelected = new EventEmitter<ChatRoomDTO>();

  constructor(private chatService: ChatService) {}

  ngOnInit() {
    this.fetchChats();
  }

  fetchChats() {
    this.loading = true;
    this.chatService.getUserChats().subscribe({
      next: (rooms) => {
        this.chats = rooms.sort((a, b) => new Date(b.lastActivity).getTime() - new Date(a.lastActivity).getTime());
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  selectRoom(room: ChatRoomDTO) {
    this.selectedRoomId = room.roomId;
    this.roomSelected.emit(room);
  }

  formatName(name: string): string {
    if (!name) return '';
    return name.length > 20 ? `${name.substring(0, 17)}...` : name;
  }

  getInitials(name: string): string {
    if (!name) return '';
    return name.split(' ').map(p => p[0]).join('').toUpperCase().substring(0, 2);
  }

  formatTime(dateString: string): string {
    const date = new Date(dateString);
    if (isNaN(date.getTime())) return '';

    const now = new Date();
    const diffDays = Math.floor((now.getTime() - date.getTime()) / (1000 * 60 * 60 * 24));

    if (diffDays === 0) return `Aujourd'hui ${date.toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}`;
    if (diffDays === 1) return `Hier ${date.toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}`;
    if (date.getFullYear() === now.getFullYear()) return date.toLocaleDateString([], {month: 'short', day: 'numeric'});

    return date.toLocaleDateString();
  }

  getLastMessagePreview(room: ChatRoomDTO): string {
    // Utilisez lastActivity comme fallback
    return `Dernière activité: ${this.formatTime(room.lastActivity)}`;

    // Ou pour un texte plus simple:
    // return "Cliquez pour voir les messages";
  }
}