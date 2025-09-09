// support.models.ts
export interface ChatRoomDTO {
    id: number;
    roomId: string;
    type: 'PRIVATE' | 'GROUP' | 'SUPPORT';
    name: string;
    description?: string;
    avatar?: string;
    lastActivity: string;
    participants: ChatParticipantDTO[];
    unreadCount: number;
    lastMessage?: ChatMessageDTO;
    
    // Champs spécifiques au support
    supportTicketId?: string;
    supportStatus?: SupportStatus;
    supportType?: SupportType;
    isSupportChat?: boolean;
}

export interface ChatMessageDTO {
    id: number;
    content: string;
    type: 'TEXT' | 'IMAGE' | 'FILE' | 'AUDIO' | 'VIDEO';
    senderId: number;
    senderName: string;
    senderAvatar?: string;
    timestamp: string;
    editedAt?: string;
    isEdited: boolean;
    isDeleted?: boolean;
    replyToId?: number;
    replyToMessage?: ChatMessageDTO;
    status?: 'SENT' | 'DELIVERED' | 'READ';

    // Champs pour fichiers / Cloudinary
    fileName?: string;
    fileUrl?: string;
    cloudinarySecureUrl?: string;
    cloudinaryPublicId?: string;
    fileSize?: number;
    mimeType?: string;
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

// Dans votre fichier support.models.ts, mettez à jour l'interface :

export interface CreateSupportTicketDTO {
    supportType: SupportType;
    subject: string;
    description: string;
    priority?: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
}

// Ajoutez aussi ces nouvelles interfaces :


export interface SupportTicketResponse {
    ticket: SupportTicketDTO;
    message: string;
    chatRoomId: string;
}

export interface SupportDashboardStatsDTO {
    totalTickets: number;
    openTickets: number;
    inProgressTickets: number;
    resolvedTickets: number;
    closedTickets: number;
    myAssignedTickets: number;
    urgentTickets: number;
    avgResolutionTimeHours: number;
}

export interface SupportTicketDTO {
    id: number;
    ticketId: string;
    subject: string;
    description: string;
    supportType: SupportType;
    status: SupportStatus;
    priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';

    // Informations utilisateur
    userId: number;
    userName: string;
    userEmail?: string;

    // Informations admin
    assignedAdminId?: number;
    assignedAdminName?: string;

    // Informations ChatRoom associée
    chatRoomId?: string;
    unreadMessagesCount: number;

    // Dates
    createdAt: Date;
    updatedAt?: Date;
    resolvedAt?: Date;
    closedAt?: Date;
}

export interface SupportTypeResponse {
    value: SupportType;
    displayName: string;
}

export interface CreateSupportTicketRequest {
    supportType: SupportType;
    subject: string;
    description: string;
}