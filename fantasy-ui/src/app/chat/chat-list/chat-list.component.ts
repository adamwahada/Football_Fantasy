import {Component, EventEmitter, OnInit, Output} from '@angular/core';
import {ChatRoomDTO} from "../chat.models";
import {ChatService} from "../service/chat.service";
import {NgForOf, NgIf} from "@angular/common";

@Component({
  selector: 'app-chat-list',
  imports: [
    NgForOf,
    NgIf
  ],
  templateUrl: './chat-list.component.html',
  styleUrl: './chat-list.component.scss',
  standalone:true,
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
        this.chats = rooms.sort((a, b) => b.lastActivity.localeCompare(a.lastActivity));
      },
      error: () => {},
      complete: () => this.loading = false
    });
  }

  selectRoom(room: ChatRoomDTO) {
    this.selectedRoomId = room.roomId;
    this.roomSelected.emit(room);
  }
}