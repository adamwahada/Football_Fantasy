package FootballFantasy.fantasy.Services.ChatService;

import FootballFantasy.fantasy.Dto.ChatDto.*;
import FootballFantasy.fantasy.Entities.Chat.*;
import FootballFantasy.fantasy.Entities.UserEntity.UserEntity;
import FootballFantasy.fantasy.Repositories.ChatRepository.ChatMessageRepository;
import FootballFantasy.fantasy.Repositories.ChatRepository.ChatParticipantRepository;
import FootballFantasy.fantasy.Repositories.ChatRepository.ChatRoomRepository;
import FootballFantasy.fantasy.Repositories.ChatRepository.MessageStatusRepository;
import FootballFantasy.fantasy.Repositories.UserRepository.UserRepository;
import FootballFantasy.fantasy.Services.Cloudinary.CloudinaryFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final MessageStatusRepository messageStatusRepository;
    private final UserRepository userRepository;

    @Autowired
    private CloudinaryFileService cloudinaryFileService;

    // Créer ou récupérer un chat privé
    public ChatRoomDTO getOrCreatePrivateChat(Long userId1, Long userId2) {
        Optional<ChatRoom> existingRoom = chatRoomRepository.findPrivateChatRoom(userId1, userId2, ChatRoomType.PRIVATE);

        if (existingRoom.isPresent()) {
            return convertToDTO(existingRoom.get(), userId1);
        }

        // Créer nouvelle room
        ChatRoom chatRoom = ChatRoom.builder()
                .roomId(UUID.randomUUID().toString())
                .type(ChatRoomType.PRIVATE)
                .lastActivity(LocalDateTime.now())
                .build();

        chatRoom = chatRoomRepository.save(chatRoom);

        // Ajouter les participants
        UserEntity user1 = userRepository.findById(userId1).orElseThrow();
        UserEntity user2 = userRepository.findById(userId2).orElseThrow();

        ChatParticipant participant1 = ChatParticipant.builder()
                .user(user1)
                .chatRoom(chatRoom)
                .role(ParticipantRole.MEMBER)
                .lastSeenAt(LocalDateTime.now())
                .build();

        ChatParticipant participant2 = ChatParticipant.builder()
                .user(user2)
                .chatRoom(chatRoom)
                .role(ParticipantRole.MEMBER)
                .lastSeenAt(LocalDateTime.now())
                .build();

        chatParticipantRepository.save(participant1);
        chatParticipantRepository.save(participant2);

        return convertToDTO(chatRoom, userId1);
    }

    public ChatRoomDTO createOrGetPrivateChat(Long currentUserId, Long otherUserId) {
        // Chercher d'abord un chat privé existant
        Optional<ChatRoom> existingRoom = chatRoomRepository.findPrivateChatRoom(currentUserId, otherUserId, ChatRoomType.PRIVATE);

        if (existingRoom.isPresent()) {
            return convertToDTO(existingRoom.get(), currentUserId);
        }

        // Sinon, utiliser la méthode existante
        return getOrCreatePrivateChat(currentUserId, otherUserId);
    }

    // Créer un groupe
    public ChatRoomDTO createGroup(String name, String description, Long creatorId, List<Long> participantIds) {
        ChatRoom chatRoom = ChatRoom.builder()
                .roomId(UUID.randomUUID().toString())
                .type(ChatRoomType.GROUP)
                .name(name)
                .description(description)
                .lastActivity(LocalDateTime.now())
                .build();

        chatRoom = chatRoomRepository.save(chatRoom);

        // Ajouter le créateur comme admin
        UserEntity creator = userRepository.findById(creatorId).orElseThrow();
        ChatParticipant creatorParticipant = ChatParticipant.builder()
                .user(creator)
                .chatRoom(chatRoom)
                .role(ParticipantRole.ADMIN)
                .lastSeenAt(LocalDateTime.now())
                .build();
        chatParticipantRepository.save(creatorParticipant);

        // Ajouter les autres participants
        for (Long participantId : participantIds) {
            if (!participantId.equals(creatorId)) {
                UserEntity user = userRepository.findById(participantId).orElseThrow();
                ChatParticipant participant = ChatParticipant.builder()
                        .user(user)
                        .chatRoom(chatRoom)
                        .role(ParticipantRole.MEMBER)
                        .lastSeenAt(LocalDateTime.now())
                        .build();
                chatParticipantRepository.save(participant);
            }
        }

        return convertToDTO(chatRoom, creatorId);
    }

    // Envoyer un message
    public ChatMessageDTO sendMessage(SendMessageDTO messageDto, Long senderId) {
        ChatRoom chatRoom = chatRoomRepository.findByRoomId(messageDto.getRoomId())
                .orElseThrow(() -> new RuntimeException("Chat room not found"));

        UserEntity sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!chatParticipantRepository.existsByRoomIdAndUserIdAndIsActiveTrue(messageDto.getRoomId(), senderId)) {
            throw new RuntimeException("User is not a participant of this chat");
        }

        ChatMessage message = ChatMessage.builder()
                .content(messageDto.getContent())
                .type(messageDto.getType())
                .sender(sender)
                .chatRoom(chatRoom)
                .isDeleted(false)
                .isEdited(false)
                .build();

        if (messageDto.getReplyToId() != null) {
            ChatMessage replyTo = chatMessageRepository.findById(messageDto.getReplyToId()).orElse(null);
            message.setReplyTo(replyTo);
        }

        message = chatMessageRepository.save(message);

        // Mettre à jour l'activité de la room
        chatRoom.updateLastActivity();
        chatRoomRepository.save(chatRoom);

        // Créer les statuts pour tous les participants
        List<ChatParticipant> participants = chatParticipantRepository.findByChatRoomIdAndIsActiveTrue(chatRoom.getId());
        for (ChatParticipant participant : participants) {
            MessageStatusType status = participant.getUser().getId().equals(senderId) ?
                    MessageStatusType.READ : MessageStatusType.SENT;

            MessageStatus messageStatus = MessageStatus.builder()
                    .message(message)
                    .user(participant.getUser())
                    .status(status)
                    .build();
            messageStatusRepository.save(messageStatus);
        }

        return convertToMessageDTO(message, senderId);
    }

    // ✅ MÉTHODE PRINCIPALE POUR L'UPLOAD DE FICHIERS AVEC CLOUDINARY
    public ChatMessageDTO sendFileMessage(String roomId, MultipartFile file, Long senderId) {
        // Validations initiales
        ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));

        UserEntity sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!chatParticipantRepository.existsByRoomIdAndUserIdAndIsActiveTrue(roomId, senderId)) {
            throw new RuntimeException("User is not a participant of this chat");
        }

        try {
            // 📂 Upload du fichier vers Cloudinary
            String secureUrl;
            MessageType messageType;

            // Déterminer le type de message basé sur le MIME type
            String contentType = file.getContentType();
            if (contentType != null) {
                if (contentType.startsWith("image")) {
                    secureUrl = cloudinaryFileService.uploadImage(file, "chat/images");
                    messageType = MessageType.IMAGE;
                } else if (contentType.startsWith("video")) {
                    secureUrl = cloudinaryFileService.uploadFile(file, "chat/videos");
                    messageType = MessageType.VIDEO;
                } else if (contentType.startsWith("audio")) {
                    secureUrl = cloudinaryFileService.uploadFile(file, "chat/audio");
                    messageType = MessageType.AUDIO;
                } else {
                    secureUrl = cloudinaryFileService.uploadFile(file, "chat/files");
                    messageType = MessageType.FILE;
                }
            } else {
                secureUrl = cloudinaryFileService.uploadFile(file, "chat/files");
                messageType = MessageType.FILE;
            }

            // Extraire l'ID public pour le stockage
            String publicId = cloudinaryFileService.extractPublicId(secureUrl);

            // 📌 Créer le message avec les informations Cloudinary
            ChatMessage message = ChatMessage.builder()
                    .content(file.getOriginalFilename()) // Nom du fichier comme contenu
                    .type(messageType)
                    .sender(sender)
                    .chatRoom(chatRoom)
                    .fileName(file.getOriginalFilename())
                    .fileUrl(secureUrl) // URL HTTP standard
                    .cloudinarySecureUrl(secureUrl) // URL HTTPS sécurisée
                    .cloudinaryPublicId(publicId) // ID public pour la suppression
                    .fileSize(file.getSize())
                    .mimeType(file.getContentType())
                    .isDeleted(false)
                    .isEdited(false)
                    .build();

            message = chatMessageRepository.save(message);

            // 📌 Mettre à jour l'activité de la room
            chatRoom.updateLastActivity();
            chatRoomRepository.save(chatRoom);

            // 📌 Gérer les statuts des messages
            List<ChatParticipant> participants = chatParticipantRepository
                    .findByChatRoomIdAndIsActiveTrue(chatRoom.getId());

            for (ChatParticipant participant : participants) {
                MessageStatusType status = participant.getUser().getId().equals(senderId) ?
                        MessageStatusType.READ : MessageStatusType.SENT;

                messageStatusRepository.save(MessageStatus.builder()
                        .message(message)
                        .user(participant.getUser())
                        .status(status)
                        .build());
            }

            return convertToMessageDTO(message, senderId);

        } catch (IOException e) {
            throw new RuntimeException("File upload to Cloudinary failed: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Error processing file message: " + e.getMessage(), e);
        }
    }

    // Récupérer les messages d'une room
    public Page<ChatMessageDTO> getRoomMessages(String roomId, Long userId, Pageable pageable) {
        ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));

        if (!chatParticipantRepository.existsByRoomIdAndUserIdAndIsActiveTrue(roomId, userId)) {
            throw new RuntimeException("User is not a participant of this chat");
        }

        Page<ChatMessage> messages = chatMessageRepository.findByChatRoomIdAndIsDeletedFalseOrderByTimestampDesc(
                chatRoom.getId(), pageable);

        return messages.map(message -> convertToMessageDTO(message, userId));
    }

    // Récupérer les chats de l'utilisateur
    public List<ChatRoomDTO> getUserChats(Long userId) {
        List<ChatRoom> chatRooms = chatRoomRepository.findUserChatRooms(userId);
        return chatRooms.stream()
                .map(room -> convertToDTO(room, userId))
                .collect(Collectors.toList());
    }

    // Marquer comme lu
    public void markAsRead(String roomId, Long messageId, Long userId) {
        Optional<MessageStatus> statusOpt = messageStatusRepository.findByMessageIdAndUserId(messageId, userId);
        if (statusOpt.isPresent()) {
            MessageStatus status = statusOpt.get();
            status.setStatus(MessageStatusType.READ);
            status.setTimestamp(LocalDateTime.now());
            messageStatusRepository.save(status);
        }
    }

    public Long getUserIdByKeycloakId(String keycloakId) {
        UserEntity user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getId();
    }

    public List<ChatMessageDTO> searchMessages(String roomId, String query, Long userId) {
        ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));

        if (!chatParticipantRepository.existsByRoomIdAndUserIdAndIsActiveTrue(roomId, userId)) {
            throw new RuntimeException("User is not a participant of this chat");
        }

        List<ChatMessage> messages = chatMessageRepository
                .findByChatRoomIdAndContentContainingIgnoreCaseAndIsDeletedFalse(chatRoom.getId(), query);

        return messages.stream()
                .map(message -> convertToMessageDTO(message, userId))
                .collect(Collectors.toList());
    }

    public ChatMessageDTO editMessage(Long messageId, String newContent, Long userId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (!message.getSender().getId().equals(userId)) {
            throw new RuntimeException("Only sender can edit the message");
        }

        if (message.getIsDeleted()) {
            throw new RuntimeException("Cannot edit deleted message");
        }

        if (newContent == null || newContent.trim().isEmpty()) {
            throw new RuntimeException("Message content cannot be empty");
        }

        message.setContent(newContent.trim());
        message.setIsEdited(true);
        message.setEditedAt(LocalDateTime.now());

        message = chatMessageRepository.save(message);
        return convertToMessageDTO(message, userId);
    }

    // ✅ MÉTHODE POUR SUPPRIMER UN MESSAGE AVEC FICHIER (VERSION CLOUDINARY)
    // ✅ MÉTHODE POUR SUPPRIMER UN MESSAGE AVEC FICHIER (VERSION CLOUDINARY CORRIGÉE)
    public void deleteMessageWithFile(Long messageId, Long userId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        // Vérifier que l'utilisateur peut supprimer ce message
        if (!message.getSender().getId().equals(userId)) {
            throw new RuntimeException("You can only delete your own messages");
        }

        // Si c'est un fichier, supprimer de Cloudinary avec le bon resource_type
        if (message.getType() != MessageType.TEXT && message.getCloudinaryPublicId() != null) {
            boolean deleted = false;

            // Déterminer le resource_type basé sur le type de message
            if (message.getType() == MessageType.IMAGE) {
                deleted = cloudinaryFileService.deleteFile(message.getCloudinaryPublicId(), "image");
            } else {
                // Pour VIDEO, AUDIO, FILE -> utiliser "raw"
                deleted = cloudinaryFileService.deleteFile(message.getCloudinaryPublicId(), "raw");
            }

            if (!deleted) {
                log.warn("Failed to delete file from Cloudinary: {}", message.getCloudinaryPublicId());
            }
        }

        // Marquer le message comme supprimé
        message.setIsDeleted(true);
        message.setContent("[Message deleted]");
        chatMessageRepository.save(message);
    }
    public void addParticipants(String roomId, List<Long> userIds, Long adminId) {
        ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));

        if (chatRoom.getType() != ChatRoomType.GROUP) {
            throw new RuntimeException("Can only add participants to groups");
        }

        ChatParticipant adminParticipant = chatParticipantRepository
                .findByRoomIdAndUserId(roomId, adminId)
                .orElseThrow(() -> new RuntimeException("User is not a participant"));

        if (adminParticipant.getRole() != ParticipantRole.ADMIN) {
            throw new RuntimeException("Only admins can add participants");
        }

        for (Long userId : userIds) {
            if (!chatParticipantRepository.existsByRoomIdAndUserIdAndIsActiveTrue(roomId, userId)) {
                UserEntity user = userRepository.findById(userId).orElseThrow();
                ChatParticipant participant = ChatParticipant.builder()
                        .user(user)
                        .chatRoom(chatRoom)
                        .role(ParticipantRole.MEMBER)
                        .lastSeenAt(LocalDateTime.now())
                        .build();
                chatParticipantRepository.save(participant);
            }
        }
    }

    public void removeParticipant(String roomId, Long participantId, Long adminId) {
        ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));

        if (chatRoom.getType() != ChatRoomType.GROUP) {
            throw new RuntimeException("Can only remove participants from groups");
        }

        ChatParticipant adminParticipant = chatParticipantRepository
                .findByRoomIdAndUserId(roomId, adminId)
                .orElseThrow(() -> new RuntimeException("User is not a participant"));

        if (adminParticipant.getRole() != ParticipantRole.ADMIN) {
            throw new RuntimeException("Only admins can remove participants");
        }

        ChatParticipant participantToRemove = chatParticipantRepository
                .findByRoomIdAndUserId(roomId, participantId)
                .orElseThrow(() -> new RuntimeException("Participant not found"));

        participantToRemove.setIsActive(false);
        chatParticipantRepository.save(participantToRemove);
    }

    public void leaveGroup(String roomId, Long userId) {
        ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));

        if (chatRoom.getType() != ChatRoomType.GROUP) {
            throw new RuntimeException("Can only leave groups");
        }

        ChatParticipant participant = chatParticipantRepository
                .findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new RuntimeException("User is not a participant"));

        participant.setIsActive(false);
        chatParticipantRepository.save(participant);
    }

    public ChatRoomDTO updateGroupInfo(String roomId, CreateGroupDTO updateDTO, Long userId) {
        ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));

        if (chatRoom.getType() != ChatRoomType.GROUP) {
            throw new RuntimeException("Can only update group information");
        }

        ChatParticipant participant = chatParticipantRepository
                .findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new RuntimeException("User is not a participant"));

        if (participant.getRole() != ParticipantRole.ADMIN) {
            throw new RuntimeException("Only admins can update group information");
        }

        chatRoom.setName(updateDTO.getName());
        chatRoom.setDescription(updateDTO.getDescription());
        chatRoom.setAvatar(updateDTO.getAvatar());

        chatRoom = chatRoomRepository.save(chatRoom);
        return convertToDTO(chatRoom, userId);
    }

    public void updateLastSeen(String roomId, Long userId) {
        ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));

        ChatParticipant participant = chatParticipantRepository
                .findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new RuntimeException("User is not a participant"));

        participant.setLastSeenAt(LocalDateTime.now());
        chatParticipantRepository.save(participant);
    }

    public boolean isUserParticipant(String roomId, Long userId) {
        return chatParticipantRepository.existsByRoomIdAndUserIdAndIsActiveTrue(roomId, userId);
    }

    public List<ChatParticipantDTO> getActiveParticipants(String roomId) {
        ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));

        List<ChatParticipant> participants = chatParticipantRepository.findByChatRoomIdAndIsActiveTrue(chatRoom.getId());
        return participants.stream()
                .map(this::convertToParticipantDTO)
                .collect(Collectors.toList());
    }

    public void debugParticipation(String roomId, Long userId) {
        ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));

        System.out.println("ChatRoom ID: " + chatRoom.getId());
        System.out.println("ChatRoom roomId: " + chatRoom.getRoomId());
        System.out.println("User ID: " + userId);

        List<ChatParticipant> allParticipants = chatParticipantRepository.findByChatRoomIdAndIsActiveTrue(chatRoom.getId());
        System.out.println("Active participants: " + allParticipants.size());

        for (ChatParticipant p : allParticipants) {
            System.out.println("Participant - User ID: " + p.getUser().getId() + ", Active: " + p.getIsActive());
        }

        boolean exists = chatParticipantRepository.existsByChatRoomIdAndUserIdAndIsActiveTrue(chatRoom.getId(), userId);
        System.out.println("User is participant: " + exists);
    }

    // ✅ MÉTHODES DE CONVERSION MISES À JOUR AVEC LES CHAMPS FICHIERS
    private ChatRoomDTO convertToDTO(ChatRoom chatRoom, Long currentUserId) {
        List<ChatParticipant> participants = chatParticipantRepository.findByChatRoomIdAndIsActiveTrue(chatRoom.getId());
        Long unreadCount = chatMessageRepository.countUnreadMessages(chatRoom.getId(), currentUserId);

        return ChatRoomDTO.builder()
                .id(chatRoom.getId())
                .roomId(chatRoom.getRoomId())
                .type(chatRoom.getType())
                .name(chatRoom.getType() == ChatRoomType.GROUP ? chatRoom.getName() : getPrivateChatName(participants, currentUserId))
                .description(chatRoom.getDescription())
                .avatar(chatRoom.getAvatar())
                .lastActivity(chatRoom.getLastActivity())
                .participants(participants.stream().map(this::convertToParticipantDTO).collect(Collectors.toList()))
                .unreadCount(unreadCount)
                .build();
    }

    private String getPrivateChatName(List<ChatParticipant> participants, Long currentUserId) {
        return participants.stream()
                .filter(p -> !p.getUser().getId().equals(currentUserId))
                .findFirst()
                .map(p -> p.getUser().getFirstName() + " " + p.getUser().getLastName())
                .orElse("Unknown");
    }

    // ✅ MÉTHODE DE CONVERSION MESSAGE MISE À JOUR AVEC LES CHAMPS FICHIERS
    private ChatMessageDTO convertToMessageDTO(ChatMessage message, Long currentUserId) {
        MessageStatus userStatus = messageStatusRepository.findByMessageIdAndUserId(message.getId(), currentUserId)
                .orElse(null);

        return ChatMessageDTO.builder()
                .id(message.getId())
                .content(message.getContent())
                .type(message.getType())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getFirstName() + " " + message.getSender().getLastName())
                .timestamp(message.getTimestamp())
                .isEdited(message.getIsEdited())
                .replyToId(message.getReplyTo() != null ? message.getReplyTo().getId() : null)
                .status(userStatus != null ? userStatus.getStatus() : null)
                // ✅ AJOUT DES CHAMPS FICHIERS CLOUDINARY
                .fileName(message.getFileName())
                .fileUrl(message.getFileUrl())
                .fileSize(message.getFileSize())
                .mimeType(message.getMimeType())
                .cloudinaryPublicId(message.getCloudinaryPublicId())
                .cloudinarySecureUrl(message.getCloudinarySecureUrl())
                .build();
    }

    private ChatParticipantDTO convertToParticipantDTO(ChatParticipant participant) {
        return ChatParticipantDTO.builder()
                .id(participant.getId())
                .userId(participant.getUser().getId())
                .username(participant.getUser().getUsername())
                .fullName(participant.getUser().getFirstName() + " " + participant.getUser().getLastName())
                .role(participant.getRole())
                .joinedAt(participant.getJoinedAt())
                .lastSeenAt(participant.getLastSeenAt())
                .isActive(participant.getIsActive())
                .build();
    }
}