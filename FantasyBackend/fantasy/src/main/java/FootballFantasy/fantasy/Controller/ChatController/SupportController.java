package FootballFantasy.fantasy.Controller.ChatController;

import FootballFantasy.fantasy.Entities.Chat.*;
import FootballFantasy.fantasy.Services.ChatService.ChatService;
import FootballFantasy.fantasy.Dto.ChatDto.UpdateTicketStatusRequest;
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
    public ResponseEntity<?> getAllTickets(@AuthenticationPrincipal Jwt jwt) {
        try {
            String keycloakId = jwt.getSubject();
            Long userId = chatService.getUserIdByKeycloakId(keycloakId);

            log.info("Récupération des tickets pour l'admin: {}", userId);
            
            List<SupportTicketDTO> tickets = chatService.getAllSupportTickets(userId);
            log.info("Tickets récupérés: {}", tickets != null ? tickets.size() : 0);
            
            return ResponseEntity.ok(tickets != null ? tickets : List.of());
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des tickets admin: ", e);
            if (e.getMessage() != null && e.getMessage().contains("Access denied")) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "Access denied - Admin only"));
            }
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Erreur interne: " + e.getMessage()));
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

            log.info("Récupération du dashboard pour l'admin: {}", adminId);

            // Récupérer les statistiques
            SupportDashboardStatsDTO stats = chatService.getSupportDashboardStats(adminId);
            log.info("Statistiques récupérées: {}", stats);

            // Récupérer les tickets récents
            List<SupportTicketDTO> recentTickets = chatService.getAllSupportTickets(adminId);
            if (recentTickets != null && recentTickets.size() > 10) {
                recentTickets = recentTickets.stream()
                        .limit(10)
                        .toList();
            }
            log.info("Tickets récents récupérés: {}", recentTickets != null ? recentTickets.size() : 0);

            Map<String, Object> response = Map.of(
                    "statistics", stats,
                    "recentTickets", recentTickets != null ? recentTickets : List.of()
            );

            log.info("Réponse dashboard préparée avec succès");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Erreur lors de la récupération du dashboard: ", e);
            if (e.getMessage() != null && e.getMessage().contains("Access denied")) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "Access denied - Admin only"));
            }
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Erreur interne du serveur: " + e.getMessage()));
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

    /**
     * ✅ Endpoint de test pour vérifier la connectivité
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testEndpoint(@AuthenticationPrincipal Jwt jwt) {
        try {
            String keycloakId = jwt.getSubject();
            Long userId = chatService.getUserIdByKeycloakId(keycloakId);
            boolean isAdmin = chatService.isUserAdmin(userId);
            
            return ResponseEntity.ok(Map.of(
                    "status", "OK",
                    "userId", userId,
                    "isAdmin", isAdmin,
                    "timestamp", java.time.LocalDateTime.now()
            ));
        } catch (Exception e) {
            log.error("Erreur dans l'endpoint de test: ", e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "ERROR",
                    "error", e.getMessage(),
                    "timestamp", java.time.LocalDateTime.now()
            ));
        }
    }

    // ✅ NOUVEAUX ENDPOINTS POUR LA GESTION DES STATUTS PAR L'ADMIN

    /**
     * ✅ Changer le statut d'un ticket (Admin seulement)
     */
    @PutMapping("/admin/ticket/{ticketId}/status")
    public ResponseEntity<?> updateTicketStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String ticketId,
            @RequestBody UpdateTicketStatusRequest request) {

        try {
            String keycloakId = jwt.getSubject();
            Long adminId = chatService.getUserIdByKeycloakId(keycloakId);

            SupportTicketDTO updatedTicket = chatService.updateTicketStatus(
                    ticketId,
                    request.getStatus(),
                    request.getPriority(),
                    request.getAdminNote(),
                    adminId
            );

            return ResponseEntity.ok(Map.of(
                    "ticket", updatedTicket,
                    "message", "Statut du ticket mis à jour avec succès"
            ));

        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour du statut: ", e);
            if (e.getMessage().contains("Access denied")) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "Accès refusé - Admin uniquement"));
            }
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * ✅ Récupérer les tickets par statut (Admin seulement)
     */
    @GetMapping("/admin/tickets/status/{status}")
    public ResponseEntity<?> getTicketsByStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String status) {

        try {
            String keycloakId = jwt.getSubject();
            Long adminId = chatService.getUserIdByKeycloakId(keycloakId);

            SupportStatus supportStatus = SupportStatus.valueOf(status.toUpperCase());
            List<SupportTicketDTO> tickets = chatService.getTicketsByStatus(supportStatus, adminId);

            return ResponseEntity.ok(Map.of(
                    "tickets", tickets,
                    "status", status,
                    "count", tickets.size()
            ));

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des tickets par statut: ", e);
            if (e.getMessage().contains("Access denied")) {
                return ResponseEntity.status(403).build();
            }
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * ✅ Récupérer les tickets actifs (Admin seulement)
     */
    @GetMapping("/admin/tickets/active")
    public ResponseEntity<?> getActiveTickets(@AuthenticationPrincipal Jwt jwt) {
        try {
            String keycloakId = jwt.getSubject();
            Long adminId = chatService.getUserIdByKeycloakId(keycloakId);

            List<SupportTicketDTO> tickets = chatService.getActiveTickets(adminId);

            return ResponseEntity.ok(Map.of(
                    "tickets", tickets,
                    "count", tickets.size(),
                    "type", "active"
            ));

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des tickets actifs: ", e);
            if (e.getMessage().contains("Access denied")) {
                return ResponseEntity.status(403).build();
            }
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * ✅ Récupérer les tickets fermés (Admin seulement)
     */
    @GetMapping("/admin/tickets/closed")
    public ResponseEntity<?> getClosedTickets(@AuthenticationPrincipal Jwt jwt) {
        try {
            String keycloakId = jwt.getSubject();
            Long adminId = chatService.getUserIdByKeycloakId(keycloakId);

            List<SupportTicketDTO> tickets = chatService.getClosedTickets(adminId);

            return ResponseEntity.ok(Map.of(
                    "tickets", tickets,
                    "count", tickets.size(),
                    "type", "closed"
            ));

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des tickets fermés: ", e);
            if (e.getMessage().contains("Access denied")) {
                return ResponseEntity.status(403).build();
            }
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * ✅ Récupérer les tickets urgents (Admin seulement)
     */
    @GetMapping("/admin/tickets/urgent")
    public ResponseEntity<?> getUrgentTickets(@AuthenticationPrincipal Jwt jwt) {
        try {
            String keycloakId = jwt.getSubject();
            Long adminId = chatService.getUserIdByKeycloakId(keycloakId);

            List<SupportTicketDTO> tickets = chatService.getUrgentTickets(adminId);

            return ResponseEntity.ok(Map.of(
                    "tickets", tickets,
                    "count", tickets.size(),
                    "type", "urgent"
            ));

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des tickets urgents: ", e);
            if (e.getMessage().contains("Access denied")) {
                return ResponseEntity.status(403).build();
            }
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * ✅ Récupérer les tickets par priorité (Admin seulement)
     */
    @GetMapping("/admin/tickets/priority/{priority}")
    public ResponseEntity<?> getTicketsByPriority(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String priority) {

        try {
            String keycloakId = jwt.getSubject();
            Long adminId = chatService.getUserIdByKeycloakId(keycloakId);

            TicketPriority ticketPriority = TicketPriority.valueOf(priority.toUpperCase());
            List<SupportTicketDTO> tickets = chatService.getTicketsByPriority(ticketPriority, adminId);

            return ResponseEntity.ok(Map.of(
                    "tickets", tickets,
                    "priority", priority,
                    "count", tickets.size()
            ));

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des tickets par priorité: ", e);
            if (e.getMessage().contains("Access denied")) {
                return ResponseEntity.status(403).build();
            }
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * ✅ Rechercher des tickets (Admin seulement)
     */
    @GetMapping("/admin/tickets/search")
    public ResponseEntity<?> searchTickets(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam String query) {

        try {
            String keycloakId = jwt.getSubject();
            Long adminId = chatService.getUserIdByKeycloakId(keycloakId);

            List<SupportTicketDTO> tickets = chatService.searchTickets(query, adminId);

            return ResponseEntity.ok(Map.of(
                    "tickets", tickets,
                    "query", query,
                    "count", tickets.size()
            ));

        } catch (Exception e) {
            log.error("Erreur lors de la recherche de tickets: ", e);
            if (e.getMessage().contains("Access denied")) {
                return ResponseEntity.status(403).build();
            }
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * ✅ Obtenir les statistiques détaillées (Admin seulement)
     */
    @GetMapping("/admin/statistics/detailed")
    public ResponseEntity<?> getDetailedStatistics(@AuthenticationPrincipal Jwt jwt) {
        try {
            String keycloakId = jwt.getSubject();
            Long adminId = chatService.getUserIdByKeycloakId(keycloakId);

            Map<String, Object> stats = chatService.getDetailedAdminStats(adminId);

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            log.error("Erreur lors de la récupération des statistiques: ", e);
            if (e.getMessage().contains("Access denied")) {
                return ResponseEntity.status(403).build();
            }
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * ✅ Assigner un ticket à l'admin (Admin seulement)
     */
    @PostMapping("/admin/ticket/{ticketId}/assign")
    public ResponseEntity<?> assignTicketToAdmin(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String ticketId) {

        try {
            String keycloakId = jwt.getSubject();
            Long adminId = chatService.getUserIdByKeycloakId(keycloakId);

            SupportTicketDTO assignedTicket = chatService.assignTicketToAdmin(ticketId, adminId);

            return ResponseEntity.ok(Map.of(
                    "ticket", assignedTicket,
                    "message", "Ticket assigné avec succès"
            ));

        } catch (Exception e) {
            log.error("Erreur lors de l'assignation du ticket: ", e);
            if (e.getMessage().contains("Access denied")) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "Accès refusé - Admin uniquement"));
            }
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * ✅ Récupérer les priorités disponibles
     */
    @GetMapping("/priorities")
    public ResponseEntity<List<Map<String, String>>> getPriorities() {
        List<Map<String, String>> priorities = List.of(
                Map.of("value", "LOW", "displayName", "Basse"),
                Map.of("value", "MEDIUM", "displayName", "Moyenne"),
                Map.of("value", "HIGH", "displayName", "Élevée"),
                Map.of("value", "URGENT", "displayName", "Urgente")
        );

        return ResponseEntity.ok(priorities);
    }

    /**
     * ✅ Récupérer les statuts disponibles
     */
    @GetMapping("/statuses")
    public ResponseEntity<List<Map<String, String>>> getStatuses() {
        List<Map<String, String>> statuses = List.of(
                Map.of("value", "OPEN", "displayName", "Ouvert"),
                Map.of("value", "IN_PROGRESS", "displayName", "En cours"),
                Map.of("value", "RESOLVED", "displayName", "Résolu"),
                Map.of("value", "CLOSED", "displayName", "Fermé")
        );

        return ResponseEntity.ok(statuses);
    }
}