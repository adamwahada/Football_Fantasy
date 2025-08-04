package FootballFantasy.fantasy.Controller.ChatController;

import FootballFantasy.fantasy.Dto.ChatDto.*;
import FootballFantasy.fantasy.Services.ChatService.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload SendMessageDTO messageDTO, Principal principal) {
        try {
            Long senderId = chatService.getUserIdByKeycloakId(principal.getName());
            ChatMessageDTO message = chatService.sendMessage(messageDTO, senderId);

            // Send to room subscribers
            messagingTemplate.convertAndSend(
                    "/topic/room/" + messageDTO.getRoomId(),
                    WebSocketMessage.builder()
                            .type("MESSAGE")
                            .roomId(messageDTO.getRoomId())
                            .senderId(senderId)
                            .payload(message)
                            .build()
            );

            log.info("Message sent to room {} by user {}", messageDTO.getRoomId(), senderId);
        } catch (Exception e) {
            log.error("Error sending message: ", e);
        }
    }

    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload TypingIndicatorDTO typingDTO, Principal principal) {
        try {
            Long userId = chatService.getUserIdByKeycloakId(principal.getName());
            typingDTO.setUserId(userId);

            // Broadcast typing indicator to room
            messagingTemplate.convertAndSend(
                    "/topic/room/" + typingDTO.getRoomId() + "/typing",
                    typingDTO
            );

        } catch (Exception e) {
            log.error("Error handling typing indicator: ", e);
        }
    }

    @MessageMapping("/chat.markAsRead")
    public void markAsRead(@Payload MarkAsReadDTO markAsReadDTO, Principal principal) {
        try {
            Long userId = chatService.getUserIdByKeycloakId(principal.getName());
            chatService.markAsRead(markAsReadDTO.getRoomId(), markAsReadDTO.getMessageId(), userId);

            // Notify room about read status
            messagingTemplate.convertAndSend(
                    "/topic/room/" + markAsReadDTO.getRoomId() + "/status",
                    WebSocketMessage.builder()
                            .type("MESSAGE_READ")
                            .roomId(markAsReadDTO.getRoomId())
                            .senderId(userId)
                            .payload(markAsReadDTO)
                            .build()
            );

        } catch (Exception e) {
            log.error("Error marking message as read: ", e);
        }
    }

    @MessageMapping("/chat.joinRoom")
    public void joinRoom(@Payload String roomId, Principal principal) {
        try {
            Long userId = chatService.getUserIdByKeycloakId(principal.getName());

            // Update user's last seen in the room
            chatService.updateLastSeen(roomId, userId);

            // Notify room that user joined
            messagingTemplate.convertAndSend(
                    "/topic/room/" + roomId + "/activity",
                    WebSocketMessage.builder()
                            .type("USER_JOINED")
                            .roomId(roomId)
                            .senderId(userId)
                            .build()
            );

        } catch (Exception e) {
            log.error("Error joining room: ", e);
        }
    }

    @MessageMapping("/chat.leaveRoom")
    public void leaveRoom(@Payload String roomId, Principal principal) {
        try {
            Long userId = chatService.getUserIdByKeycloakId(principal.getName());

            // Update user's last seen in the room
            chatService.updateLastSeen(roomId, userId);

            // Notify room that user left
            messagingTemplate.convertAndSend(
                    "/topic/room/" + roomId + "/activity",
                    WebSocketMessage.builder()
                            .type("USER_LEFT")
                            .roomId(roomId)
                            .senderId(userId)
                            .build()
            );

        } catch (Exception e) {
            log.error("Error leaving room: ", e);
        }
    }
}