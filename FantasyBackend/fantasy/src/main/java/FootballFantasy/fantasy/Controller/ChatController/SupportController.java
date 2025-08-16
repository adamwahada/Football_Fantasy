package FootballFantasy.fantasy.Controller.ChatController;

import FootballFantasy.fantasy.Dto.ChatDto.ChatRoomDTO;
import FootballFantasy.fantasy.Entities.Chat.SupportType;
import FootballFantasy.fantasy.Services.ChatService.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
@Slf4j
public class SupportController {

    private final ChatService chatService;

    /**
     * ✅ Créer un nouveau ticket de support
     */
    @PostMapping("/ticket")
    public ResponseEntity<ChatRoomDTO> createSupportTicket(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody CreateSupportTicketRequest request) {

        try {
            String keycloakId = jwt.getSubject();
            Long userId = chatService.getUserIdByKeycloakId(keycloakId);

            ChatRoomDTO ticket = chatService.createSupportTicket(
                    userId,
                    request.getSupportType(),
                    request.getSubject(),
                    request.getDescription()
            );

            return ResponseEntity.ok(ticket);
        } catch (Exception e) {
            log.error("Error creating support ticket: ", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * ✅ Récupérer les tickets de l'utilisateur connecté
     */
    @GetMapping("/my-tickets")
    public ResponseEntity<List<ChatRoomDTO>> getMyTickets(@AuthenticationPrincipal Jwt jwt) {
        try {
            String keycloakId = jwt.getSubject();
            Long userId = chatService.getUserIdByKeycloakId(keycloakId);

            List<ChatRoomDTO> tickets = chatService.getUserSupportTickets(userId);
            return ResponseEntity.ok(tickets);
        } catch (Exception e) {
            log.error("Error getting user tickets: ", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * ✅ Récupérer tous les tickets (pour admin seulement)
     */
    @GetMapping("/admin/tickets")
    public ResponseEntity<List<ChatRoomDTO>> getAllTickets(@AuthenticationPrincipal Jwt jwt) {
        try {
            String keycloakId = jwt.getSubject();
            Long userId = chatService.getUserIdByKeycloakId(keycloakId);

            // Vérifier si c'est un admin
            if (!chatService.isUserAdmin(userId)) {
                return ResponseEntity.status(403).build(); // Forbidden
            }

            List<ChatRoomDTO> tickets = chatService.getAdminSupportTickets(userId);
            return ResponseEntity.ok(tickets);
        } catch (Exception e) {
            log.error("Error getting admin tickets: ", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * ✅ Marquer un ticket comme résolu (admin seulement)
     */
    @PutMapping("/admin/ticket/{roomId}/resolve")
    public ResponseEntity<Map<String, String>> resolveTicket(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String roomId) {

        try {
            String keycloakId = jwt.getSubject();
            Long adminId = chatService.getUserIdByKeycloakId(keycloakId);

            // Vérifier si c'est un admin
            if (!chatService.isUserAdmin(adminId)) {
                return ResponseEntity.status(403).build(); // Forbidden
            }

            chatService.resolveSupportTicket(roomId, adminId);

            return ResponseEntity.ok(Map.of("message", "Ticket résolu avec succès"));
        } catch (Exception e) {
            log.error("Error resolving ticket: ", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * ✅ Récupérer les types de support disponibles
     */
    @GetMapping("/types")
    public ResponseEntity<List<SupportTypeResponse>> getSupportTypes() {
        List<SupportTypeResponse> types = List.of(
                new SupportTypeResponse(SupportType.PAYMENT, SupportType.PAYMENT.getDisplayName()),
                new SupportTypeResponse(SupportType.TECHNICAL, SupportType.TECHNICAL.getDisplayName()),
                new SupportTypeResponse(SupportType.ACCOUNT, SupportType.ACCOUNT.getDisplayName()),
                new SupportTypeResponse(SupportType.GENERAL, SupportType.GENERAL.getDisplayName())
        );

        return ResponseEntity.ok(types);
    }

    /**
     * ✅ Vérifier si l'utilisateur connecté est admin
     */
    @GetMapping("/is-admin")
    public ResponseEntity<Map<String, Boolean>> isAdmin(@AuthenticationPrincipal Jwt jwt) {
        try {
            String keycloakId = jwt.getSubject();
            Long userId = chatService.getUserIdByKeycloakId(keycloakId);

            boolean isAdmin = chatService.isUserAdmin(userId);
            return ResponseEntity.ok(Map.of("isAdmin", isAdmin));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("isAdmin", false));
        }
    }

    // ✅ Classes pour les requêtes et réponses
    public static class CreateSupportTicketRequest {
        private SupportType supportType;
        private String subject;
        private String description;

        // Constructeurs
        public CreateSupportTicketRequest() {}

        public CreateSupportTicketRequest(SupportType supportType, String subject, String description) {
            this.supportType = supportType;
            this.subject = subject;
            this.description = description;
        }

        // Getters et Setters
        public SupportType getSupportType() { return supportType; }
        public void setSupportType(SupportType supportType) { this.supportType = supportType; }

        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public static class SupportTypeResponse {
        private SupportType value;
        private String displayName;

        public SupportTypeResponse(SupportType value, String displayName) {
            this.value = value;
            this.displayName = displayName;
        }

        // Getters
        public SupportType getValue() { return value; }
        public String getDisplayName() { return displayName; }
    }
    @GetMapping("/admin/dashboard")
    public ResponseEntity<List<ChatRoomDTO>> getSupportDashboard(@AuthenticationPrincipal Jwt jwt) {
        Long adminId = chatService.getUserIdByKeycloakId(jwt.getSubject());
        List<ChatRoomDTO> tickets = chatService.getAdminSupportDashboard(adminId);
        return ResponseEntity.ok(tickets);
    }
}