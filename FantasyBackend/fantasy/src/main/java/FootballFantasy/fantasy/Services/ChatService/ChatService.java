package FootballFantasy.fantasy.Services.ChatService;

import FootballFantasy.fantasy.Dto.ChatDto.ChatUserDTO;
import FootballFantasy.fantasy.Dto.ChatDto.MessageDTO;
import FootballFantasy.fantasy.Dto.ChatDto.SendMessageRequest;
import FootballFantasy.fantasy.Entities.Chat.Message;
import FootballFantasy.fantasy.Entities.Chat.MessageStatus;
import FootballFantasy.fantasy.Entities.Chat.WebSocketMessage;
import FootballFantasy.fantasy.Entities.UserEntity.UserEntity;
import FootballFantasy.fantasy.Repositories.ChatRepository.MessageRepository;
import FootballFantasy.fantasy.Repositories.UserRepository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SimpMessageSendingOperations messagingTemplate;

    // Pour suivre les utilisateurs connectés
    private final Set<Long> onlineUsers = ConcurrentHashMap.newKeySet();

    @Transactional
    public MessageDTO sendMessage(Long senderId, SendMessageRequest request) {
        UserEntity sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Expéditeur non trouvé"));

        UserEntity receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new RuntimeException("Destinataire non trouvé"));

        Message message = Message.builder()
                .content(request.getContent())
                .sender(sender)
                .receiver(receiver)
                .sentAt(LocalDateTime.now())
                .isRead(false)
                .status(MessageStatus.SENT)
                .build();

        Message savedMessage = messageRepository.save(message);
        MessageDTO messageDTO = convertToDTO(savedMessage);

        // Envoyer le message via WebSocket au destinataire
        messagingTemplate.convertAndSendToUser(
                receiver.getId().toString(),
                "/queue/messages",
                WebSocketMessage.builder()
                        .type("MESSAGE")
                        .message(messageDTO)
                        .build()
        );

        // Envoyer confirmation à l'expéditeur
        messagingTemplate.convertAndSendToUser(
                sender.getId().toString(),
                "/queue/messages",
                WebSocketMessage.builder()
                        .type("MESSAGE_SENT")
                        .message(messageDTO)
                        .build()
        );

        log.info("Message envoyé de {} vers {}", sender.getUsername(), receiver.getUsername());
        return messageDTO;
    }

    public List<MessageDTO> getMessagesBetweenUsers(Long user1Id, Long user2Id) {
        UserEntity user1 = userRepository.findById(user1Id)
                .orElseThrow(() -> new RuntimeException("Utilisateur 1 non trouvé"));
        UserEntity user2 = userRepository.findById(user2Id)
                .orElseThrow(() -> new RuntimeException("Utilisateur 2 non trouvé"));

        List<Message> messages = messageRepository.findMessagesBetweenUsers(user1, user2);
        return messages.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ChatUserDTO> getChatUsers(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        List<UserEntity> chatPartners = messageRepository.findChatPartners(user);

        return chatPartners.stream()
                .map(partner -> {
                    List<Message> lastMessages = messageRepository.findLastMessageBetweenUsers(user, partner);
                    Message lastMessage = lastMessages.isEmpty() ? null : lastMessages.get(0);

                    List<Message> unreadMessages = messageRepository.findUnreadMessages(user);
                    int unreadCount = (int) unreadMessages.stream()
                            .filter(msg -> msg.getSender().getId().equals(partner.getId()))
                            .count();

                    return ChatUserDTO.builder()
                            .id(partner.getId())
                            .username(partner.getUsername())
                            .firstName(partner.getFirstName())
                            .lastName(partner.getLastName())
                            .lastMessage(lastMessage != null ? lastMessage.getContent() : "")
                            .lastMessageTime(lastMessage != null ? lastMessage.getSentAt() : null)
                            .unreadCount(unreadCount)
                            .isOnline(onlineUsers.contains(partner.getId()))
                            .build();
                })
                .sorted((a, b) -> {
                    if (a.getLastMessageTime() == null && b.getLastMessageTime() == null) return 0;
                    if (a.getLastMessageTime() == null) return 1;
                    if (b.getLastMessageTime() == null) return -1;
                    return b.getLastMessageTime().compareTo(a.getLastMessageTime());
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void markMessagesAsRead(Long userId, Long senderId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        UserEntity sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Expéditeur non trouvé"));

        List<Message> unreadMessages = messageRepository.findMessagesBetweenUsers(sender, user)
                .stream()
                .filter(msg -> msg.getReceiver().getId().equals(userId) && !msg.getIsRead())
                .collect(Collectors.toList());

        unreadMessages.forEach(msg -> {
            msg.setIsRead(true);
            msg.setStatus(MessageStatus.READ);
        });

        messageRepository.saveAll(unreadMessages);

        // Notifier l'expéditeur que ses messages ont été lus
        messagingTemplate.convertAndSendToUser(
                senderId.toString(),
                "/queue/messages",
                WebSocketMessage.builder()
                        .type("MESSAGES_READ")
                        .userId(userId)
                        .build()
        );
    }

    public void userConnected(Long userId) {
        onlineUsers.add(userId);
    }

    public void userDisconnected(Long userId) {
        onlineUsers.remove(userId);
    }

    public boolean isUserOnline(Long userId) {
        return onlineUsers.contains(userId);
    }

    public void handleTyping(Long userId, Long receiverId, boolean isTyping) {
        messagingTemplate.convertAndSendToUser(
                receiverId.toString(),
                "/queue/typing",
                WebSocketMessage.builder()
                        .type("TYPING")
                        .userId(userId)
                        .data(isTyping)
                        .build()
        );
    }

    private MessageDTO convertToDTO(Message message) {
        return MessageDTO.builder()
                .id(message.getId())
                .content(message.getContent())
                .senderId(message.getSender().getId())
                .senderUsername(message.getSender().getUsername())
                .senderFirstName(message.getSender().getFirstName())
                .senderLastName(message.getSender().getLastName())
                .receiverId(message.getReceiver().getId())
                .receiverUsername(message.getReceiver().getUsername())
                .receiverFirstName(message.getReceiver().getFirstName())
                .receiverLastName(message.getReceiver().getLastName())
                .sentAt(message.getSentAt())
                .isRead(message.getIsRead())
                .status(message.getStatus())
                .build();
    }
}
