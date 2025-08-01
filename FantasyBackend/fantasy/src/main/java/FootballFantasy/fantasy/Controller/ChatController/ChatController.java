package FootballFantasy.fantasy.Controller.ChatController;

import FootballFantasy.fantasy.Dto.ChatDto.ChatUserDTO;
import FootballFantasy.fantasy.Dto.ChatDto.MessageDTO;
import FootballFantasy.fantasy.Dto.ChatDto.SendMessageRequest;
import FootballFantasy.fantasy.Entities.UserEntity.UserEntity;
import FootballFantasy.fantasy.Repositories.UserRepository.UserRepository;
import FootballFantasy.fantasy.Services.ChatService.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;
    private final UserRepository userRepository;

    @PostMapping("/send")
    public ResponseEntity<MessageDTO> sendMessage(
            @Valid @RequestBody SendMessageRequest request,
            Authentication authentication) {

        // 🔍 DEBUG : Afficher les informations d'authentification
        log.info("=== DEBUG AUTHENTICATION ===");
        log.info("Authentication: {}", authentication);
        log.info("Is Authenticated: {}", authentication != null ? authentication.isAuthenticated() : "null");
        log.info("Username: {}", authentication != null ? authentication.getName() : "null");
        log.info("Authorities: {}", authentication != null ? authentication.getAuthorities() : "null");
        log.info("Details: {}", authentication != null ? authentication.getDetails() : "null");
        log.info("Principal: {}", authentication != null ? authentication.getPrincipal() : "null");
        log.info("============================");

        try {
            // Récupérer l'ID de l'utilisateur depuis l'authentification
            Long senderId = getUserIdFromAuthentication(authentication);
            log.info("Sender ID trouvé: {}", senderId);

            MessageDTO message = chatService.sendMessage(senderId, request);
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du message: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/messages/{userId}")
    public ResponseEntity<List<MessageDTO>> getMessages(
            @PathVariable Long userId,
            Authentication authentication) {

        Long currentUserId = getUserIdFromAuthentication(authentication);
        log.info("Récupérer les messages entre user connecté ID={} et userId={}", currentUserId, userId);

        List<MessageDTO> messages = chatService.getMessagesBetweenUsers(currentUserId, userId);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/users")
    public ResponseEntity<List<ChatUserDTO>> getChatUsers(Authentication authentication) {
        Long userId = getUserIdFromAuthentication(authentication);
        List<ChatUserDTO> chatUsers = chatService.getChatUsers(userId);
        return ResponseEntity.ok(chatUsers);
    }

    @PutMapping("/messages/read/{senderId}")
    public ResponseEntity<Void> markMessagesAsRead(
            @PathVariable Long senderId,
            Authentication authentication) {

        Long userId = getUserIdFromAuthentication(authentication);
        chatService.markMessagesAsRead(userId, senderId);
        return ResponseEntity.ok().build();
    }

   // @GetMessage("/online/{userId}")
    //public ResponseEntity<Boolean> isUserOnline(@PathVariable Long userId) {
        //boolean isOnline = chatService.isUserOnline(userId);
       // return ResponseEntity.ok(isOnline);
   // }

    // 🔍 MÉTHODE DEBUG POUR TESTER SANS AUTHENTIFICATION
    @PostMapping("/send-test")
    public ResponseEntity<MessageDTO> sendMessageTest(
            @Valid @RequestBody SendMessageRequest request,
            @RequestParam Long senderId) {

        log.info("=== TEST MODE ===");
        log.info("Sender ID: {}", senderId);
        log.info("Receiver ID: {}", request.getReceiverId());
        log.info("Content: {}", request.getContent());

        MessageDTO message = chatService.sendMessage(senderId, request);
        return ResponseEntity.ok(message);
    }

    private Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("Utilisateur non authentifié");
            throw new RuntimeException("Utilisateur non authentifié");
        }

        String keycloakId = authentication.getName(); // C'est en fait le Keycloak ID !
        log.info("Recherche de l'utilisateur avec keycloakId: '{}'", keycloakId);

        // 🔍 DEBUG : Lister tous les utilisateurs
        List<UserEntity> allUsers = userRepository.findAll();
        log.info("=== TOUS LES UTILISATEURS DANS LA BASE ===");
        allUsers.forEach(user ->
                log.info("ID: {}, Username: '{}', KeycloakId: '{}'",
                        user.getId(), user.getUsername(),
                        user.getKeycloakId() != null ? user.getKeycloakId() : "null")
        );
        log.info("=======================================");

        // 🚀 CHERCHER PAR KEYCLOAK ID AU LIEU DE USERNAME !
        UserEntity user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> {
                    log.error("Utilisateur non trouvé pour keycloakId: '{}'", keycloakId);
                    return new RuntimeException("Utilisateur non trouvé pour keycloakId: " + keycloakId);
                });

        log.info("Utilisateur trouvé: ID={}, Username='{}', KeycloakId='{}'",
                user.getId(), user.getUsername(), user.getKeycloakId());
        return user.getId();
    }
}