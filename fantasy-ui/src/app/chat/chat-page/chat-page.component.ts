import { Component } from '@angular/core';
import {ChatRoomDTO} from "../chat.models";
import {ChatListComponent} from "../chat-list/chat-list.component";
import {ChatRoomComponent} from "../chat-room/chat-room.component";

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
export class ChatPageComponent {
  selectedRoom?: ChatRoomDTO;

  onRoomSelected(room: ChatRoomDTO) {
    this.selectedRoom = room;
  }
}
