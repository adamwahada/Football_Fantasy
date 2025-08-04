package FootballFantasy.fantasy.Services.ChatService;

import FootballFantasy.fantasy.Configuration.WebSocketEventListener;
import FootballFantasy.fantasy.Dto.ChatDto.ChatMessageDTO;
import FootballFantasy.fantasy.Dto.ChatDto.TypingIndicatorDTO;
import FootballFantasy.fantasy.Entities.Chat.ChatParticipant;
import FootballFantasy.fantasy.Repositories.ChatRepository.ChatParticipantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatParticipantRepository chatParticipantRepository;
    private final WebSocketEventListener webSocketEventListener;

    public void sendMessageNotification(ChatMessageDTO message, String roomId) {
        List<ChatParticipant> participants = chatParticipantRepository.findByChatRoomIdAndIsActiveTrue(
                Long.parseLong(roomId) // This needs to be adapted based on your room ID type
        );

        for (ChatParticipant participant : participants) {
            // Skip sender
            if (participant.getUser().getId().equals(message.getSenderId())) {
                continue;
            }

            String userId = participant.getUser().getId().toString();

            // Send real-time notification if user is online
            if (webSocketEventListener.isUserOnline(userId)) {
                messagingTemplate.convertAndSendToUser(
                        userId,
                        "/queue/notifications",
                        message
                );
            }

            // Here you could also send push notifications, emails, etc.
            log.info("Notification sent for message {} to user {}", message.getId(), userId);
        }
    }

    public void sendTypingNotification(String roomId, Long userId, String username, boolean isTyping) {
        messagingTemplate.convertAndSend(
                "/topic/room/" + roomId + "/typing",
                TypingIndicatorDTO.builder()
                        .roomId(roomId)
                        .userId(userId)
                        .username(username)
                        .isTyping(isTyping)
                        .build()
        );
    }
}
