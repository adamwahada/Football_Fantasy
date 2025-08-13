import { Component, ElementRef, Input, OnInit, ViewChild } from '@angular/core';
import { ChatMessageDTO, ChatParticipantDTO, ChatRoomDTO, SendMessageDTO } from "../chat.models";
import { ChatService } from "../service/chat.service";
import { FormsModule } from "@angular/forms";
import { DatePipe, NgClass, NgForOf, NgIf } from "@angular/common";
import { AuthService } from "../../core/services/auth.service";
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';

@Component({
  selector: 'app-chat-room',
  standalone: true,
  imports: [FormsModule, NgIf, NgForOf, NgClass, DatePipe],
  templateUrl: './chat-room.component.html',
  styleUrls: ['./chat-room.component.scss']
})
export class ChatRoomComponent implements OnInit {
  @Input() room?: ChatRoomDTO;
  messages: ChatMessageDTO[] = [];
  participants: ChatParticipantDTO[] = [];
  messageContent = '';
  replyToMsg?: ChatMessageDTO;
  loading = false;
  fileToSend?: File;
  imagePreviewUrl?: string;

  currentUserId: number = 0;
  editingMsgId: number | null = null;
  editedContent: string = '';
  actionsMenuOpened: number | null = null;

  @ViewChild('messagesContainer') messagesContainer!: ElementRef;

  constructor(
      private chatService: ChatService,
      private authService: AuthService,
      private sanitizer: DomSanitizer
  ) {}

  async ngOnInit() {
    try {
      this.currentUserId = await this.authService.getRealUserId();

      if (this.currentUserId <= 0) {
        await this.authService.refreshUserId();
        this.currentUserId = await this.authService.getRealUserId();

        if (this.currentUserId <= 0) {
          this.showError('Impossible de récupérer votre identifiant utilisateur');
          return;
        }
      }

      if (this.room) {
        await this.loadRoom();
      }
    } catch (error) {
      console.error('Error initializing chat room:', error);
      this.showError('Erreur lors du chargement de la discussion');
    }
  }

  async ngOnChanges() {
    if (this.currentUserId <= 0) {
      this.currentUserId = await this.authService.getRealUserId();
    }

    if (this.room) {
      await this.loadRoom();
    }
  }

  private showError(message: string) {
    // Simple alert replacement - you can enhance this later
    alert(message);
  }

  private showSuccess(message: string) {
    // Simple alert replacement - you can enhance this later
    console.log(message);
  }

  isMine(msg: ChatMessageDTO): boolean {
    return Number(msg.senderId) === Number(this.currentUserId);
  }

  async loadRoom() {
    if (!this.room) return;

    this.loading = true;
    this.messages = [];

    try {
      // Charger les messages
      this.chatService.getRoomMessages(this.room.roomId).subscribe({
        next: (res) => {
          this.messages = res.content.reverse();
          this.scrollToBottom();
        },
        error: (error) => {
          console.error('Error loading messages:', error);
          this.showError('Erreur lors du chargement des messages');
        },
        complete: () => {
          this.loading = false;
        }
      });

      // Charger les participants
      this.chatService.getActiveParticipants(this.room.roomId).subscribe({
        next: (data) => {
          this.participants = data;
        },
        error: (error) => {
          console.error('Error loading participants:', error);
        }
      });

    } catch (error) {
      console.error('Error loading room:', error);
      this.loading = false;
      this.showError('Erreur lors du chargement de la discussion');
    }
  }

  async sendMessage() {
    if (!this.canSendMessage()) {
      return;
    }

    if (!this.room) {
      this.showError('Aucune discussion sélectionnée');
      return;
    }

    if (this.currentUserId <= 0) {
      this.currentUserId = await this.authService.getRealUserId();
      if (this.currentUserId <= 0) {
        this.showError('Impossible d\'identifier l\'utilisateur');
        return;
      }
    }

    try {
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
          this.messages.push(msg);
          this.messageContent = '';
          this.replyToMsg = undefined;
          this.fileToSend = undefined;
          this.scrollToBottom();
        },
        error: (error) => {
          console.error('Error sending message:', error);
          this.showError('Erreur lors de l\'envoi du message');
        }
      });
    } catch (error) {
      console.error('Error sending message:', error);
      this.showError('Erreur lors de l\'envoi du message');
    }
  }

  onFileSelected(event: any) {
    const file: File = event.target.files[0];
    if (file) {
      if (file.size > 10 * 1024 * 1024) { // 10MB limit
        this.showError('Le fichier est trop volumineux (max 10MB)');
        return;
      }

      this.fileToSend = file;

      // Prévisualisation si c'est une image
      if (file.type.match('image.*')) {
        const reader = new FileReader();
        reader.onload = (e: any) => {
          this.imagePreviewUrl = e.target.result;
        };
        reader.readAsDataURL(file);
      }
    }
  }

  clearFile() {
    this.fileToSend = undefined;
    this.imagePreviewUrl = undefined;
  }

  canSendMessage(): boolean {
    return !!this.messageContent.trim() || !!this.fileToSend;
  }

  getRoomInitials(): string {
    if (!this.room?.name) return '?';
    const names = this.room.name.split(' ');
    return names.length > 1
        ? `${names[0][0]}${names[names.length - 1][0]}`.toUpperCase()
        : names[0][0].toUpperCase();
  }

  getSenderInitials(msg: ChatMessageDTO): string {
    if (!msg.senderName) return '?';
    const names = msg.senderName.split(' ');
    return names.length > 1
        ? `${names[0][0]}${names[names.length - 1][0]}`.toUpperCase()
        : names[0][0].toUpperCase();
  }

  getReplySender(messageId: number): string {
    const message = this.messages.find(m => m.id === messageId);
    return message?.senderName || 'Utilisateur inconnu';
  }

  getReplyContent(messageId: number): string {
    const message = this.messages.find(m => m.id === messageId);
    if (!message) return 'Message supprimé';

    if (message.type === 'IMAGE') return '📷 Image';
    if (message.type === 'FILE') return '📁 Fichier';

    return message.content.length > 30
        ? message.content.substring(0, 30) + '...'
        : message.content || 'Message supprimé';
  }

  getFileUrl(msg: ChatMessageDTO): SafeUrl {
    return this.sanitizer.bypassSecurityTrustUrl('http://localhost:9090/uploads/chat/' + msg.content);
  }

  isImage(filename: string): boolean {
    return /\.(jpg|jpeg|png|gif|webp|bmp)$/i.test(filename);
  }

  scrollToBottom() {
    setTimeout(() => {
      if (this.messagesContainer) {
        this.messagesContainer.nativeElement.scrollTop = this.messagesContainer.nativeElement.scrollHeight;
      }
    }, 100);
  }

  showMessageActions(msgId: number) {
    this.actionsMenuOpened = msgId;
  }

  hideMessageActions(msgId: number) {
    if (this.actionsMenuOpened === msgId) {
      this.actionsMenuOpened = null;
    }
  }

  openImagePreview(imageUrl: string) {
    this.imagePreviewUrl = 'http://localhost:9090/uploads/chat/' + imageUrl;
  }

  closeImagePreview() {
    this.imagePreviewUrl = undefined;
  }

  closeActionsMenu() {
    this.actionsMenuOpened = null;
  }

  replyToMessage(msg: ChatMessageDTO) {
    this.replyToMsg = msg;
    this.actionsMenuOpened = null;
    this.scrollToBottom();
  }

  cancelReply() {
    this.replyToMsg = undefined;
  }

  startEditMessage(msg: ChatMessageDTO) {
    if (!this.isMine(msg)) {
      this.showError('Vous ne pouvez modifier que vos propres messages');
      return;
    }

    this.editingMsgId = msg.id;
    this.editedContent = msg.content;
    this.actionsMenuOpened = null;
  }

  cancelEditMessage() {
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
      this.showError('Vous ne pouvez modifier que vos propres messages');
      this.cancelEditMessage();
      return;
    }

    this.chatService.editMessage(msg.id, newContent).subscribe({
      next: (updated: ChatMessageDTO) => {
        const idx = this.messages.findIndex(m => m.id === msg.id);
        if (idx !== -1) {
          this.messages[idx] = updated;
        }
        this.cancelEditMessage();
      },
      error: (error) => {
        console.error('Error editing message:', error);
        this.showError('Erreur lors de la modification du message');
        this.cancelEditMessage();
      }
    });
  }

  deleteMessage(msg: ChatMessageDTO) {
    if (!this.isMine(msg)) {
      this.showError('Vous ne pouvez supprimer que vos propres messages');
      return;
    }

    if (confirm('Voulez-vous vraiment supprimer ce message ? Cette action est irréversible.')) {
      this.chatService.deleteMessage(msg.id).subscribe({
        next: () => {
          this.messages = this.messages.filter(m => m.id !== msg.id);
          this.showSuccess('Message supprimé');
        },
        error: (error) => {
          console.error('Error deleting message:', error);
          this.showError('Erreur lors de la suppression du message');
        }
      });
    }
  }
}