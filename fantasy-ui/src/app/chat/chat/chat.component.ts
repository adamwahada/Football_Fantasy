import { Component, OnInit } from '@angular/core';
import { ChatService } from '../chat.service';
import {FormsModule} from "@angular/forms";
import {DatePipe} from "@angular/common";

@Component({
  selector: 'app-chat',
  templateUrl: './chat.component.html',
  styleUrls: ['./chat.component.scss'],
  standalone: true,
  imports: [
    FormsModule,
    DatePipe
  ], // ajoute FormsModule et CommonModule dans app.module ou ici si standalone
})
export class ChatComponent implements OnInit {
  username = '';
  targetUser = '';
  message = '';
  messages: any[] = [];
  connected = false;

  constructor(private chatService: ChatService) {}

  ngOnInit(): void {}

  connect() {
    if (this.username.trim()) {
      this.chatService.connect(this.username);
      this.chatService.getMessages().subscribe((msg) => {
        this.messages.push(msg);
      });
      this.connected = true;
    }
  }

  send() {
    if (this.message.trim() && this.targetUser.trim()) {
      this.chatService.sendMessage(this.username, this.targetUser, this.message);
      // Affichage immédiat côté client
      this.messages.push({
        sender: this.username,
        receiver: this.targetUser,
        content: this.message,
        timestamp: new Date().toISOString(),
      });
      this.message = '';
    }
  }
}
