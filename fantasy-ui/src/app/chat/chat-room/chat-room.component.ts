import { Component, ElementRef, Input, OnInit, ViewChild, OnChanges } from '@angular/core';
import { ChatMessageDTO, ChatParticipantDTO, ChatRoomDTO, SendMessageDTO } from "../models/chat.models";
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
export class ChatRoomComponent implements OnInit, OnChanges {
  @Input() room?: ChatRoomDTO;
  messages: ChatMessageDTO[] = [];
  participants: ChatParticipantDTO[] = [];
  messageContent = '';
  replyToMsg?: ChatMessageDTO;
  loading = false;
  fileToSend?: File;
  filePreviewUrl?: string;
  isUploading = false;

  // Image preview modal
  imagePreviewUrl?: string;
  imagePreviewSender?: string;
  imagePreviewDate?: string;
  imagePreviewDownload?: string;

  // Audio player state
  audioStates: Map<number, {
    isPlaying: boolean;
    duration: string;
    currentTime: number;
  }> = new Map();

  // User and UI state
  currentUserId: number = 0;
  editingMsgId: number | null = null;
  editedContent: string = '';
  actionsMenuOpened: number | null = null;

  // Typing indicator
  someoneIsTyping = false;
  typingUserName = '';
  private typingTimer: any;

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
    alert(message);
  }

  private showSuccess(message: string) {
    console.log(message);
  }

  // ==================== MESSAGE UTILITIES ====================

  isMine(msg: ChatMessageDTO): boolean {
    return Number(msg.senderId) === Number(this.currentUserId);
  }

  isImageMessage(msg: ChatMessageDTO): boolean {
    return msg.type === 'IMAGE';
  }

  isFileMessage(msg: ChatMessageDTO): boolean {
    return msg.type === 'FILE' || msg.type === 'AUDIO' || msg.type === 'VIDEO';
  }

  isImageFile(file: File): boolean {
    return file.type.startsWith('image/');
  }

  isVideoFile(file: File): boolean {
    return file.type.startsWith('video/');
  }

  isAudioFile(file: File): boolean {
    return file.type.startsWith('audio/');
  }

  // ==================== ROOM LOADING ====================

  async loadRoom() {
    if (!this.room) return;

    this.loading = true;
    this.messages = [];

    try {
      // Charger les messages
      this.chatService.getRoomMessages(this.room.roomId).subscribe({
        next: (res) => {
          this.messages = res.content.reverse();
          this.initializeAudioStates();
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

  // ==================== MESSAGE SENDING ====================

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

    this.isUploading = true;

    try {
      let obs;
      if (this.fileToSend) {
        // Upload de fichier via Cloudinary
        obs = this.chatService.uploadFile(this.room.roomId, this.fileToSend);
      } else {
        // Message texte
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
          this.filePreviewUrl = undefined;
          this.isUploading = false;
          this.scrollToBottom();

          // Initialiser l'état audio si nécessaire
          if (msg.type === 'AUDIO') {
            this.initializeAudioState(msg.id);
          }
        },
        error: (error) => {
          console.error('Error sending message:', error);
          this.showError('Erreur lors de l\'envoi du message');
          this.isUploading = false;
        }
      });
    } catch (error) {
      console.error('Error sending message:', error);
      this.showError('Erreur lors de l\'envoi du message');
      this.isUploading = false;
    }
  }

  // ==================== FILE HANDLING ====================

  onFileSelected(event: any) {
    const file: File = event.target.files[0];
    if (file) {
      this.handleFileSelection(file);
    }
    // Reset input
    event.target.value = '';
  }

  onImageSelected(event: any) {
    const file: File = event.target.files[0];
    if (file && file.type.startsWith('image/')) {
      this.handleFileSelection(file);
    }
    // Reset input
    event.target.value = '';
  }

  private handleFileSelection(file: File) {
    // Vérifier la taille (10MB max)
    if (file.size > 10 * 1024 * 1024) {
      this.showError('Le fichier est trop volumineux (max 10MB)');
      return;
    }

    this.fileToSend = file;

    // Créer une prévisualisation pour les images
    if (this.isImageFile(file)) {
      const reader = new FileReader();
      reader.onload = (e: any) => {
        this.filePreviewUrl = e.target.result;
      };
      reader.readAsDataURL(file);
    }
  }

  clearFile() {
    this.fileToSend = undefined;
    this.filePreviewUrl = undefined;
  }

  canSendMessage(): boolean {
    return !!this.messageContent.trim() || !!this.fileToSend;
  }

  // ==================== URL GETTERS ====================

  getImageUrl(msg: ChatMessageDTO): string {
    // Priorité à cloudinarySecureUrl, puis fileUrl, puis fallback
    if (msg.cloudinarySecureUrl) {
      return msg.cloudinarySecureUrl;
    }
    if (msg.fileUrl) {
      return msg.fileUrl;
    }
    // Fallback vers l'ancienne méthode
    return `http://localhost:9090/uploads/chat/${msg.content}`;
  }

  getVideoUrl(msg: ChatMessageDTO): string {
    return this.getImageUrl(msg); // Même logique
  }

  getAudioUrl(msg: ChatMessageDTO): string {
    return this.getImageUrl(msg); // Même logique
  }

  getFileUrl(msg: ChatMessageDTO): SafeUrl {
    const url = this.getImageUrl(msg);
    return this.sanitizer.bypassSecurityTrustUrl(url);
  }

  // ==================== DISPLAY UTILITIES ====================

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

    if (message.type === 'IMAGE') return '🖼️ Image';
    if (message.type === 'VIDEO') return '🎥 Vidéo';
    if (message.type === 'AUDIO') return '🎵 Audio';
    if (message.type === 'FILE') return `📁 ${message.fileName || 'Fichier'}`;

    return message.content.length > 30
        ? message.content.substring(0, 30) + '...'
        : message.content || 'Message supprimé';
  }

  getReplyPreview(content: string): string {
    return content.length > 30 ? content.substring(0, 30) + '...' : content;
  }

  // ==================== FILE TYPE UTILITIES ====================

  getFileIcon(mimeType?: string): string {
    return this.chatService.getFileIcon(mimeType || '');
  }

  getFileTypeIcon(mimeType: string): string {
    if (mimeType.startsWith('image/')) return '🖼️';
    if (mimeType.startsWith('video/')) return '🎥';
    if (mimeType.startsWith('audio/')) return '🎵';
    if (mimeType.includes('pdf')) return '📄';
    if (mimeType.includes('word')) return '📝';
    if (mimeType.includes('excel') || mimeType.includes('sheet')) return '📊';
    if (mimeType.includes('zip') || mimeType.includes('rar')) return '🗜️';
    return '📎';
  }

  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  }

  // ==================== AUDIO PLAYER ====================

  initializeAudioStates() {
    this.messages.forEach(msg => {
      if (msg.type === 'AUDIO') {
        this.initializeAudioState(msg.id);
      }
    });
  }

  initializeAudioState(messageId: number) {
    if (!this.audioStates.has(messageId)) {
      this.audioStates.set(messageId, {
        isPlaying: false,
        duration: '0:00',
        currentTime: 0
      });
    }
  }

  toggleAudioPlay(msg: ChatMessageDTO) {
    const audioElement = document.querySelector(`audio[data-msg-id="${msg.id}"]`) as HTMLAudioElement;
    if (!audioElement) return;

    const state = this.audioStates.get(msg.id);
    if (!state) return;

    if (state.isPlaying) {
      audioElement.pause();
      state.isPlaying = false;
    } else {
      // Pause all other audio
      this.pauseAllAudio();
      audioElement.play();
      state.isPlaying = true;
    }
  }

  pauseAllAudio() {
    this.audioStates.forEach((state, messageId) => {
      if (state.isPlaying) {
        const audioElement = document.querySelector(`audio[data-msg-id="${messageId}"]`) as HTMLAudioElement;
        if (audioElement) {
          audioElement.pause();
        }
        state.isPlaying = false;
      }
    });
  }

  isAudioPlaying(messageId: number): boolean {
    return this.audioStates.get(messageId)?.isPlaying || false;
  }

  onAudioEnded(messageId: number) {
    const state = this.audioStates.get(messageId);
    if (state) {
      state.isPlaying = false;
      state.currentTime = 0;
    }
  }

  onAudioTimeUpdate(event: any, messageId: number) {
    const audioElement = event.target;
    const state = this.audioStates.get(messageId);
    if (state) {
      state.currentTime = audioElement.currentTime;
      if (audioElement.duration) {
        state.duration = this.formatAudioTime(audioElement.duration);
      }
    }
  }

  getAudioDuration(messageId: number): string {
    return this.audioStates.get(messageId)?.duration || '0:00';
  }

  private formatAudioTime(seconds: number): string {
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  }

  // ==================== IMAGE PREVIEW ====================

  openImagePreview(imageUrl: string, msg?: ChatMessageDTO) {
    this.imagePreviewUrl = imageUrl;
    if (msg) {
      this.imagePreviewSender = msg.senderName;
      this.imagePreviewDate = msg.timestamp;
      this.imagePreviewDownload = this.getImageUrl(msg);
    }
  }

  closeImagePreview() {
    this.imagePreviewUrl = undefined;
    this.imagePreviewSender = undefined;
    this.imagePreviewDate = undefined;
    this.imagePreviewDownload = undefined;
  }

  downloadImagePreview() {
    if (this.imagePreviewDownload) {
      const link = document.createElement('a');
      link.href = this.imagePreviewDownload;
      link.download = 'image';
      link.click();
    }
  }

  // ==================== DOWNLOAD ====================

  downloadFile(msg: ChatMessageDTO) {
    const url = this.getImageUrl(msg);
    const link = document.createElement('a');
    link.href = url;
    link.download = msg.fileName || msg.content;
    link.click();
  }

  // ==================== IMAGE EVENTS ====================

  onImageLoad() {
    // Scroll to bottom when image loads
    setTimeout(() => this.scrollToBottom(), 100);
  }

  onImageError(event: any) {
    console.error('Error loading image:', event);
    // You could set a fallback image here
    event.target.src = 'assets/images/image-error.png';
  }

  // ==================== SCROLL MANAGEMENT ====================

  scrollToBottom() {
    setTimeout(() => {
      if (this.messagesContainer) {
        this.messagesContainer.nativeElement.scrollTop = this.messagesContainer.nativeElement.scrollHeight;
      }
    }, 100);
  }

  // ==================== MESSAGE ACTIONS ====================

  showMessageActions(msgId: number) {
    this.actionsMenuOpened = msgId;
  }

  hideMessageActions(msgId: number) {
    if (this.actionsMenuOpened === msgId) {
      this.actionsMenuOpened = null;
    }
  }

  closeActionsMenu() {
    this.actionsMenuOpened = null;
  }

  // ==================== REPLY FUNCTIONALITY ====================

  replyToMessage(msg: ChatMessageDTO) {
    this.replyToMsg = msg;
    this.actionsMenuOpened = null;
    this.scrollToBottom();
  }

  cancelReply() {
    this.replyToMsg = undefined;
  }

  // ==================== MESSAGE EDITING ====================

  startEditMessage(msg: ChatMessageDTO) {
    if (!this.isMine(msg) || msg.type !== 'TEXT') {
      this.showError('Seuls les messages texte peuvent être modifiés');
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

  // ==================== MESSAGE DELETION ====================

  deleteMessage(msg: ChatMessageDTO) {
    if (!this.isMine(msg)) {
      this.showError('Vous ne pouvez supprimer que vos propres messages');
      return;
    }

    const confirmMessage = msg.type === 'IMAGE' || msg.type === 'VIDEO' || msg.type === 'FILE'
        ? 'Voulez-vous vraiment supprimer ce fichier ? Il sera également supprimé du stockage cloud.'
        : 'Voulez-vous vraiment supprimer ce message ?';

    if (confirm(confirmMessage)) {
      this.chatService.deleteMessage(msg.id).subscribe({
        next: () => {
          this.messages = this.messages.filter(m => m.id !== msg.id);
          this.showSuccess('Message supprimé');

          // Clean up audio state if needed
          if (msg.type === 'AUDIO') {
            this.audioStates.delete(msg.id);
          }
        },
        error: (error) => {
          console.error('Error deleting message:', error);
          this.showError('Erreur lors de la suppression du message');
        }
      });
    }
  }

  // ==================== TYPING INDICATOR ====================

  onTyping() {
    if (this.typingTimer) {
      clearTimeout(this.typingTimer);
    }

    // In a real implementation, you would emit a typing event to the server
    // this.socketService.emitTyping(this.room?.roomId, this.currentUserId);

    this.typingTimer = setTimeout(() => {
      // Stop typing indicator after 3 seconds
      // this.socketService.emitStopTyping(this.room?.roomId, this.currentUserId);
    }, 3000);
  }

  // ==================== UTILITY METHODS ====================

  isImage(filename: string): boolean {
    return /\.(jpg|jpeg|png|gif|webp|bmp)$/i.test(filename);
  }

  // ==================== LIFECYCLE CLEANUP ====================

  ngOnDestroy() {
    if (this.typingTimer) {
      clearTimeout(this.typingTimer);
    }
    this.pauseAllAudio();
  }




}