package FootballFantasy.fantasy.Controller.ChatController;

import FootballFantasy.fantasy.Entities.Chat.*;
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
    public ResponseEntity<?> createSupportTicket(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody CreateSupportTicketRequest request) {

        try {
            // Validations de base
            if (request.getSupportType() == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Type de support requis"));
            }

            if (request.getSubject() == null || request.getSubject().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Sujet requis"));
            }

            if (request.getDescription() == null || request.getDescription().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Description requise"));
            }

            String keycloakId = jwt.getSubject();
            Long userId = chatService.getUserIdByKeycloakId(keycloakId);

            // Créer le ticket COMPLET (Ticket + ChatRoom + Messages)
            SupportTicketDTO ticket = chatService.createSupportTicketComplete(
                    userId,
                    request.getSupportType(),
                    request.getSubject().trim(),
                    request.getDescription().trim(),
                    request.getPriority()
            );

            log.info("Ticket créé avec succès: {} pour utilisateur: {}", ticket.getTicketId(), userId);

            // Retourner le ticket créé avec les informations de la ChatRoom
            return ResponseEntity.ok(Map.of(
                    "ticket", ticket,
                    "message", "Ticket créé avec succès ! Vous pouvez maintenant discuter avec notre équipe.",
                    "chatRoomId", ticket.getChatRoomId()
            ));

        } catch (Exception e) {
            log.error("Erreur lors de la création du ticket de support: ", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Erreur lors de la création du ticket: " + e.getMessage()));
        }
    }

    /**
     * ✅ Récupérer les tickets de l'utilisateur connecté (VERSION COMPLÈTE)
     */
    @GetMapping("/my-tickets")
    public ResponseEntity<List<SupportTicketDTO>> getMyTickets(@AuthenticationPrincipal Jwt jwt) {
        try {
            String keycloakId = jwt.getSubject();
            Long userId = chatService.getUserIdByKeycloakId(keycloakId);

            List<SupportTicketDTO> tickets = chatService.getUserSupportTicketsComplete(userId);
            return ResponseEntity.ok(tickets);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des tickets utilisateur: ", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * ✅ Récupérer tous les tickets (pour admin seulement) - VERSION COMPLÈTE
     */
    @GetMapping("/admin/tickets")
    public ResponseEntity<List<SupportTicketDTO>> getAllTickets(@AuthenticationPrincipal Jwt jwt) {
        try {
            String keycloakId = jwt.getSubject();
            Long userId = chatService.getUserIdByKeycloakId(keycloakId);

            List<SupportTicketDTO> tickets = chatService.getAllSupportTickets(userId);
            return ResponseEntity.ok(tickets);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des tickets admin: ", e);
            if (e.getMessage().contains("Access denied")) {
                return ResponseEntity.status(403).build();
            }
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * ✅ Dashboard admin avec statistiques détaillées
     */
    @GetMapping("/admin/dashboard")
    public ResponseEntity<?> getSupportDashboard(@AuthenticationPrincipal Jwt jwt) {
        try {
            String keycloakId = jwt.getSubject();
            Long adminId = chatService.getUserIdByKeycloakId(keycloakId);

            // Récupérer les statistiques
            SupportDashboardStatsDTO stats = chatService.getSupportDashboardStats(adminId);

            // Récupérer les tickets récents
            List<SupportTicketDTO> recentTickets = chatService.getAllSupportTickets(adminId)
                    .stream()
                    .limit(10) // Limiter à 10 tickets récents
                    .toList();

            return ResponseEntity.ok(Map.of(
                    "statistics", stats,
                    "recentTickets", recentTickets
            ));
        } catch (Exception e) {
            log.error("Erreur lors de la récupération du dashboard: ", e);
            if (e.getMessage().contains("Access denied")) {
                return ResponseEntity.status(403).build();
            }
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * ✅ Marquer un ticket comme résolu (admin seulement) - VERSION AMÉLIORÉE
     */
    @PutMapping("/admin/ticket/{roomId}/resolve")
    public ResponseEntity<Map<String, String>> resolveTicket(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String roomId) {

        try {
            String keycloakId = jwt.getSubject();
            Long adminId = chatService.getUserIdByKeycloakId(keycloakId);

            chatService.resolveSupportTicketComplete(roomId, adminId);

            return ResponseEntity.ok(Map.of(
                    "message", "Ticket résolu avec succès",
                    "status", "RESOLVED"
            ));
        } catch (Exception e) {
            log.error("Erreur lors de la résolution du ticket: ", e);
            if (e.getMessage().contains("Access denied") || e.getMessage().contains("Admin only")) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "Accès refusé - Admin uniquement"));
            }
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * ✅ Récupérer un ticket spécifique par son ID
     */
    @GetMapping("/ticket/{ticketId}")
    public ResponseEntity<?> getTicket(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String ticketId) {
        try {
            String keycloakId = jwt.getSubject();
            Long userId = chatService.getUserIdByKeycloakId(keycloakId);

            SupportTicketDTO ticket = chatService.getSupportTicketById(ticketId, userId);
            return ResponseEntity.ok(ticket);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération du ticket: ", e);
            if (e.getMessage().contains("Access denied")) {
                return ResponseEntity.status(403).build();
            }
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
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
            log.warn("Erreur lors de la vérification admin: ", e);
            return ResponseEntity.ok(Map.of("isAdmin", false));
        }
    }




}