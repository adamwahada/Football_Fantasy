package FootballFantasy.fantasy.Configuration;

import FootballFantasy.fantasy.Services.ChatService.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final SimpMessageSendingOperations messagingTemplate;
    private final ChatService chatService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String userId = headerAccessor.getFirstNativeHeader("userId");

        if (userId != null) {
            log.info("User {} connected", userId);
            chatService.userConnected(Long.parseLong(userId));

            // Notifier les autres utilisateurs que cet utilisateur est en ligne
            messagingTemplate.convertAndSend("/topic/user-status",
                    Map.of("userId", userId, "status", "ONLINE"));
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String userId = headerAccessor.getFirstNativeHeader("userId");

        if (userId != null) {
            log.info("User {} disconnected", userId);
            chatService.userDisconnected(Long.parseLong(userId));

            // Notifier les autres utilisateurs que cet utilisateur est hors ligne
            messagingTemplate.convertAndSend("/topic/user-status",
                    Map.of("userId", userId, "status", "OFFLINE"));
        }
    }
}