package FootballFantasy.fantasy.Configuration;

import FootballFantasy.fantasy.Dto.ChatDto.UserStatusDTO;
import FootballFantasy.fantasy.Services.ChatService.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;

    // Store online users
    private final ConcurrentHashMap<String, String> onlineUsers = new ConcurrentHashMap<>();

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        if (headerAccessor.getUser() != null) {
            String userId = headerAccessor.getUser().getName();
            onlineUsers.put(sessionId, userId);

            log.info("User {} connected with session {}", userId, sessionId);

            // Broadcast user online status
            UserStatusDTO statusDTO = UserStatusDTO.builder()
                    .userId(Long.parseLong(userId))
                    .isOnline(true)
                    .build();

            messagingTemplate.convertAndSend("/topic/user-status", statusDTO);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        String userId = onlineUsers.remove(sessionId);
        if (userId != null) {
            log.info("User {} disconnected from session {}", userId, sessionId);

            // Broadcast user offline status
            UserStatusDTO statusDTO = UserStatusDTO.builder()
                    .userId(Long.parseLong(userId))
                    .isOnline(false)
                    .build();

            messagingTemplate.convertAndSend("/topic/user-status", statusDTO);
        }
    }

    public boolean isUserOnline(String userId) {
        return onlineUsers.containsValue(userId);
    }
}