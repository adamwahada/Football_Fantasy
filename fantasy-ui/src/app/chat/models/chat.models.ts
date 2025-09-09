// chat.models.ts
export interface ChatRoomDTO {
    id: number;
    roomId: string;
    type: 'PRIVATE' | 'GROUP';
    name: string;
    description?: string;
    avatar?: string;
    lastActivity: string;
    participants: ChatParticipantDTO[];
    unreadCount: number;
}

export interface ChatMessageDTO {
    id: number;
    content: string;
    type: 'TEXT' | 'IMAGE' | 'FILE' | 'AUDIO' | 'VIDEO';
    senderId: number;
    senderName: string;
    timestamp: string;
    isEdited: boolean;
    replyToId?: number;
    status?: 'SENT' | 'DELIVERED' | 'READ';
}

export interface ChatParticipantDTO {
    id: number;
    userId: number;
    username: string;
    fullName: string;
    role: 'ADMIN' | 'MEMBER';
    joinedAt: string;
    lastSeenAt: string;
    isActive: boolean;
}

export interface SendMessageDTO {
    roomId: string;
    content: string;
    type: 'TEXT' | 'IMAGE' | 'FILE' | 'AUDIO' | 'VIDEO';
    replyToId?: number;
}

export interface CreateGroupDTO {
    name: string;
    description: string;
    avatar?: string;
    participantIds: number[];
}

export interface UserStatusDTO {
    userId: number;
    isOnline: boolean;
}


export interface ChatMessageDTO {
    id: number;
    content: string;
    type: 'TEXT' | 'IMAGE' | 'FILE' | 'AUDIO' | 'VIDEO';
    senderId: number;
    senderName: string;
    senderAvatar?: string;        // avatar du sender
    timestamp: string;            // ISO string
    editedAt?: string;            // date édition
    isEdited: boolean;
    isDeleted?: boolean;
    replyToId?: number;
    replyToMessage?: ChatMessageDTO;
    status?: 'SENT' | 'DELIVERED' | 'READ';

    // Nouveaux champs pour fichiers / Cloudinary
    fileName?: string;
    fileUrl?: string;
    cloudinarySecureUrl?: string;
    cloudinaryPublicId?: string;
    fileSize?: number;
    mimeType?: string;

    // 🆕 Champs pour le support
    supportTicketId?: string;
    supportStatus?: string; // 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED'
    supportType?: string;   // 'PAYMENT' | 'TECHNICAL' | 'ACCOUNT' | 'GENERAL'
    isSupportChat?: boolean;
}


export interface ChatParticipantDTO {
    id: number;
    userId: number;
    username: string;
    fullName: string;
    role: 'ADMIN' | 'MEMBER';
    joinedAt: string;
    lastSeenAt: string;
    isActive: boolean;
}

export interface SendMessageDTO {
    roomId: string;
    content: string;
    type: 'TEXT' | 'IMAGE' | 'FILE' | 'AUDIO' | 'VIDEO';
    replyToId?: number;
}

export interface CreateGroupDTO {
    name: string;
    description: string;
    avatar?: string;
    participantIds: number[];
}

export enum SupportType {
    PAYMENT = 'PAYMENT',
    TECHNICAL = 'TECHNICAL',
    ACCOUNT = 'ACCOUNT',
    GENERAL = 'GENERAL'
}

export enum SupportStatus {
    OPEN = 'OPEN',
    IN_PROGRESS = 'IN_PROGRESS',
    RESOLVED = 'RESOLVED',
    CLOSED = 'CLOSED'
}

export interface CreateSupportTicketDTO {
    supportType: SupportType;
    subject: string;
    description: string;
}

export interface SupportTicketDTO extends ChatRoomDTO {
    supportTicketId: string;
    supportStatus: SupportStatus;
    supportType: SupportType;
    isSupportChat: boolean;
}
