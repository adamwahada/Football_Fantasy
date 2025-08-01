package FootballFantasy.fantasy.Controller.ChatController;

import FootballFantasy.fantasy.Dto.ChatDto.SendMessageRequest;
import FootballFantasy.fantasy.Entities.Chat.WebSocketMessage;
import FootballFantasy.fantasy.Services.ChatService.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/chat")
public class WebSocketChatController {

    private final ChatService chatService;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload SendMessageRequest request,
                            SimpMessageHeaderAccessor headerAccessor,
                            Principal principal) {
        try {
            Long senderId = getUserIdFromPrincipal(principal);
            chatService.sendMessage(senderId, request);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du message: ", e);
        }
    }

    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload WebSocketMessage message,
                             Principal principal) {
        try {
            Long userId = getUserIdFromPrincipal(principal);
            Long receiverId = (Long) message.getData();
            boolean isTyping = Boolean.TRUE.equals(message.getData());

            chatService.handleTyping(userId, receiverId, isTyping);
        } catch (Exception e) {
            log.error("Erreur lors de la gestion du typing: ", e);
        }
    }

    private Long getUserIdFromPrincipal(Principal principal) {
        // Implémentez selon votre système d'authentification
        return 1L; // Remplacez par votre logique
    }
}