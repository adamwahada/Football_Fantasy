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