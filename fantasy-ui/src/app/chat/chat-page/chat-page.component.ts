import { Component, OnInit } from '@angular/core';
import { ChatRoomDTO } from "../models/chat.models";
import { ChatListComponent } from "../chat-list/chat-list.component";
import { ChatRoomComponent } from "../chat-room/chat-room.component";
import { ActivatedRoute } from '@angular/router';
import { ChatService } from '../service/chat.service';

@Component({
  selector: 'app-chat-page',
  imports: [
    ChatListComponent,
    ChatRoomComponent
  ],
  templateUrl: './chat-page.component.html',
  styleUrl: './chat-page.component.scss',
  standalone:true,
})
export class ChatPageComponent implements OnInit {
  selectedRoom?: ChatRoomDTO;

  constructor(
    private route: ActivatedRoute,
    private chatService: ChatService
  ) {}

  ngOnInit() {
    // Vérifier s'il y a un roomId dans les paramètres de requête
    this.route.queryParams.subscribe(params => {
      const roomId = params['roomId'];
      if (roomId) {
        this.selectRoomById(roomId);
      }
    });
  }

  onRoomSelected(room: ChatRoomDTO) {
    this.selectedRoom = room;
  }

  private selectRoomById(roomId: string) {
    // Charger la room spécifique
    this.chatService.getUserChats().subscribe({
      next: (rooms) => {
        const room = rooms.find(r => r.roomId === roomId);
        if (room) {
          this.selectedRoom = room;
        }
      },
      error: (error) => {
        console.error('Erreur lors du chargement de la room:', error);
      }
    });
  }
}
