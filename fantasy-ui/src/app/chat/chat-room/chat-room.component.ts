import {Component, ElementRef, Input, OnInit, ViewChild} from '@angular/core';
import {ChatMessageDTO, ChatParticipantDTO, ChatRoomDTO, SendMessageDTO} from "../chat.models";
import {ChatService} from "../service/chat.service";
import {FormsModule} from "@angular/forms";
import {DatePipe, NgClass, NgForOf, NgIf} from "@angular/common";
import {AuthService} from "../../core/services/auth.service";

@Component({
  selector: 'app-chat-room',
  imports: [
    FormsModule, NgIf, NgForOf, NgClass, DatePipe
  ],
  templateUrl: './chat-room.component.html',
  styleUrl: './chat-room.component.scss',
  standalone: true,
})
export class ChatRoomComponent implements OnInit {
  @Input() room?: ChatRoomDTO;
  messages: ChatMessageDTO[] = [];
  participants: ChatParticipantDTO[] = [];
  messageContent = '';
  replyToMsg?: ChatMessageDTO;
  loading = false;
  fileToSend?: File;

  currentUserId: number = 0;
  editingMsgId: number | null = null;
  editedContent: string = '';
  actionsMenuOpened: number | null = null;

  @ViewChild('messagesContainer') messagesContainer!: ElementRef;

  constructor(
      private chatService: ChatService,
      private authService: AuthService
  ) {}

  async ngOnInit() {
    console.log('🚀 ChatRoomComponent ngOnInit started');

    try {
      // TOUJOURS utiliser getRealUserId() qui fait l'appel API
      this.currentUserId = await this.authService.getRealUserId();
      console.log('🔥 CURRENT USER ID (from API):', this.currentUserId);

      // Vérification de sécurité
      if (this.currentUserId <= 0) {
        console.error('❌ Invalid user ID received, trying fallback...');

        // Essayer le fallback ou forcer un refresh
        await this.authService.refreshUserId();
        this.currentUserId = await this.authService.getRealUserId();

        if (this.currentUserId <= 0) {
          console.error('❌ Still no valid user ID, using temp ID for testing');
          // Pour le debug/test seulement
          this.currentUserId = 1;
          this.authService.setTempUserId(1);
        }
      }

      // Charger la room si elle existe
      if (this.room) {
        await this.loadRoom();
      }

    } catch (error) {
      console.error('❌ Error in ngOnInit:', error);
      // Fallback pour les tests
      this.currentUserId = 1;
    }

    console.log('✅ ChatRoomComponent initialization complete with userId:', this.currentUserId);
  }

  async ngOnChanges() {
    console.log('🔄 ChatRoomComponent ngOnChanges triggered');

    // Re-récupérer l'ID utilisateur si nécessaire
    if (this.currentUserId <= 0) {
      this.currentUserId = await this.authService.getRealUserId();
      console.log('🔄 Updated currentUserId:', this.currentUserId);
    }

    if (this.room) {
      await this.loadRoom();
    }
  }

  isMine(msg: ChatMessageDTO): boolean {
    const senderId = Number(msg.senderId);
    const currentId = Number(this.currentUserId);

    console.log('🔍 isMine check:', {
      messageId: msg.id,
      senderId: senderId,
      currentUserId: currentId,
      isEqual: senderId === currentId,
      senderName: msg.senderName
    });

    return senderId === currentId;
  }

  async loadRoom() {
    if (!this.room) return;

    console.log('📥 Loading room:', this.room.roomId);
    this.loading = true;

    try {
      // Charger les messages
      this.chatService.getRoomMessages(this.room.roomId).subscribe({
        next: (res) => {
          this.messages = res.content.reverse();
          console.log('📨 Messages loaded:', this.messages.length);
          this.scrollToBottom();
        },
        error: (error) => {
          console.error('❌ Error loading messages:', error);
        },
        complete: () => {
          this.loading = false;
        }
      });

      // Charger les participants
      this.chatService.getActiveParticipants(this.room.roomId).subscribe({
        next: (data) => {
          this.participants = data;
          console.log('👥 Participants loaded:', this.participants.length);
        },
        error: (error) => {
          console.error('❌ Error loading participants:', error);
        }
      });

    } catch (error) {
      console.error('❌ Error in loadRoom:', error);
      this.loading = false;
    }
  }

  async sendMessage() {
    if (!this.messageContent.trim() && !this.fileToSend) {
      console.log('⚠️ No content to send');
      return;
    }

    if (!this.room) {
      console.error('❌ No room selected');
      return;
    }

    // S'assurer qu'on a un ID utilisateur valide
    if (this.currentUserId <= 0) {
      console.error('❌ Invalid user ID, refreshing...');
      this.currentUserId = await this.authService.getRealUserId();

      if (this.currentUserId <= 0) {
        console.error('❌ Still no valid user ID, cannot send message');
        alert('Erreur: Impossible d\'identifier l\'utilisateur');
        return;
      }
    }

    console.log('📤 Sending message with userId:', this.currentUserId);

    let obs;
    if (this.fileToSend) {
      obs = this.chatService.uploadFile(this.room.roomId, this.fileToSend, this.currentUserId);
    } else {
      const msg: SendMessageDTO = {
        roomId: this.room.roomId,
        content: this.messageContent.trim(),
        type: 'TEXT',
        ...(this.replyToMsg ? { replyToId: this.replyToMsg.id } : {})
      };
      obs = this.chatService.sendMessage(msg);
    }

    obs.subscribe({
      next: (msg: ChatMessageDTO) => {
        console.log('✅ Message sent successfully:', msg);
        this.messages.push(msg);
        this.messageContent = '';
        this.replyToMsg = undefined;
        this.fileToSend = undefined;
        this.scrollToBottom();
      },
      error: (error) => {
        console.error('❌ Error sending message:', error);
        alert('Erreur lors de l\'envoi du message');
      }
    });
  }

  onFileSelected(event: any) {
    const file: File = event.target.files[0];
    if (file) {
      console.log('📎 File selected:', file.name);
      this.fileToSend = file;
    }
  }

  setReply(message: ChatMessageDTO) {
    this.replyToMsg = message;
    console.log('↩️ Reply set to message:', message.id);
  }

  cancelReply() {
    this.replyToMsg = undefined;
    console.log('❌ Reply cancelled');
  }

  scrollToBottom() {
    setTimeout(() => {
      if (this.messagesContainer) {
        this.messagesContainer.nativeElement.scrollTop = this.messagesContainer.nativeElement.scrollHeight;
      }
    }, 150);
  }

  getFileUrl(msg: ChatMessageDTO): string {
    return 'http://localhost:9090/uploads/chat/' + msg.content;
  }

  isImage(filename: string): boolean {
    return /\.(jpg|jpeg|png|gif)$/i.test(filename);
  }

  getReplyContent(messageId: number): string {
    const message = this.messages.find(m => m.id === messageId);
    return message?.content || 'message supprimé';
  }

  /***  ACTIONS SUR MESSAGES  ***/

  openActionsMenu(msgId: number, event: MouseEvent) {
    event.stopPropagation();
    this.actionsMenuOpened = msgId;
    console.log('🔧 Actions menu opened for message:', msgId);
  }

  closeActionsMenu() {
    this.actionsMenuOpened = null;
  }

  // SUPPRIMER
  deleteMessage(msg: ChatMessageDTO) {
    if (!this.isMine(msg)) {
      console.error('❌ Cannot delete message: not mine');
      alert('Vous ne pouvez supprimer que vos propres messages');
      return;
    }

    if (!confirm('Supprimer ce message ?')) return;

    console.log('🗑️ Deleting message:', msg.id);

    this.chatService.deleteMessage(msg.id).subscribe({
      next: () => {
        console.log('✅ Message deleted successfully');
        this.messages = this.messages.filter(m => m.id !== msg.id);
        this.closeActionsMenu();
      },
      error: (error) => {
        console.error('❌ Error deleting message:', error);
        alert('Erreur lors de la suppression du message');
      }
    });
  }

  // MODIFIER
  startEditMessage(msg: ChatMessageDTO) {
    if (!this.isMine(msg)) {
      console.error('❌ Cannot edit message: not mine');
      alert('Vous ne pouvez modifier que vos propres messages');
      return;
    }

    console.log('✏️ Starting edit for message:', msg.id);
    this.editingMsgId = msg.id;
    this.editedContent = msg.content;
    this.closeActionsMenu();
  }

  cancelEditMessage() {
    console.log('❌ Edit cancelled');
    this.editingMsgId = null;
    this.editedContent = '';
  }

  confirmEditMessage(msg: ChatMessageDTO) {
    const newContent = this.editedContent.trim();

    if (!newContent || newContent === msg.content) {
      this.cancelEditMessage();
      return;
    }

    if (!this.isMine(msg)) {
      console.error('❌ Cannot edit message: not mine');
      this.cancelEditMessage();
      return;
    }

    console.log('💾 Confirming edit for message:', msg.id);

    this.chatService.editMessage(msg.id, newContent).subscribe({
      next: (updated: ChatMessageDTO) => {
        console.log('✅ Message edited successfully');
        const idx = this.messages.findIndex(m => m.id === msg.id);
        if (idx !== -1) {
          this.messages[idx] = updated;
        }
        this.cancelEditMessage();
      },
      error: (error) => {
        console.error('❌ Error editing message:', error);
        alert('Erreur lors de la modification du message');
        this.cancelEditMessage();
      }
    });
  }

  // RÉPONDRE
  replyToMessage(msg: ChatMessageDTO) {
    this.setReply(msg);
    this.closeActionsMenu();
  }

  // Méthode de debug pour vérifier l'état
  async debugCurrentUser() {
    console.log('🐛 === CHAT ROOM DEBUG ===');
    console.log('Current User ID:', this.currentUserId);
    await this.authService.debugUserInfo();

    if (this.messages.length > 0) {
      console.log('Messages ownership check:');
      this.messages.forEach(msg => {
        console.log(`Message ${msg.id}: senderId=${msg.senderId}, isMine=${this.isMine(msg)}`);
      });
    }
    console.log('🐛 === END CHAT ROOM DEBUG ===');
  }
}