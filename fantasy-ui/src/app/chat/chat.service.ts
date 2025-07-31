import { Injectable } from '@angular/core';
import { Client, Message } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { Subject } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class ChatService {
  private stompClient: Client | null = null;
  private messageSubject = new Subject<any>();

  connect(username: string) {
    if (this.stompClient && this.stompClient.active) {
      this.stompClient.deactivate();
    }

    this.stompClient = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      reconnectDelay: 5000,
      onConnect: () => {
        console.log('Connected as', username);
        this.stompClient!.subscribe('/topic/messages/' + username, (message: Message) => {
          const msg = JSON.parse(message.body);
          this.messageSubject.next(msg);
        });
      },
      onStompError: (frame) => {
        console.error('Broker reported error: ' + frame.headers['message']);
        console.error('Details: ' + frame.body);
      },
    });

    this.stompClient.activate();
  }

  sendMessage(sender: string, receiver: string, content: string) {
    if (!this.stompClient || !this.stompClient.connected) {
      console.error('Client is not connected');
      return;
    }

    const chatMessage = { sender, receiver, content, timestamp: new Date().toISOString() };
    this.stompClient.publish({
      destination: '/app/chat',
      body: JSON.stringify(chatMessage),
    });
  }

  getMessages() {
    return this.messageSubject.asObservable();
  }
}
