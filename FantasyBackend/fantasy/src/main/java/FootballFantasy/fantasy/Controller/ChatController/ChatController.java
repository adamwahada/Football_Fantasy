package FootballFantasy.fantasy.Controller.ChatController;

import FootballFantasy.fantasy.Dto.ChatDto.*;
import FootballFantasy.fantasy.Services.ChatService.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Chat", description = "Chat management API")
@SecurityRequirement(name = "bearerAuth")
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "Get user chats")
    @GetMapping("/rooms")
    public ResponseEntity<List<ChatRoomDTO>> getUserChats(Authentication authentication) {
        Long userId = extractUserIdFromAuth(authentication);
        List<ChatRoomDTO> chats = chatService.getUserChats(userId);
        return ResponseEntity.ok(chats);
    }

    @Operation(summary = "Create or get private chat")
    @PostMapping("/rooms/private/{otherUserId}")
    public ResponseEntity<ChatRoomDTO> createOrGetPrivateChat(
            @PathVariable Long otherUserId,
            Authentication authentication) {
        Long userId = extractUserIdFromAuth(authentication);
        ChatRoomDTO chatRoom = chatService.getOrCreatePrivateChat(userId, otherUserId);
        return ResponseEntity.ok(chatRoom);
    }

    @Operation(summary = "Create group chat")
    @PostMapping("/rooms/group")
    public ResponseEntity<ChatRoomDTO> createGroup(
            @Valid @RequestBody CreateGroupDTO createGroupDTO,
            Authentication authentication) {
        Long userId = extractUserIdFromAuth(authentication);
        ChatRoomDTO chatRoom = chatService.createGroup(
                createGroupDTO.getName(),
                createGroupDTO.getDescription(),
                userId,
                createGroupDTO.getParticipantIds()
        );
        return ResponseEntity.ok(chatRoom);
    }

    @Operation(summary = "Send message")
    @PostMapping("/messages")
    public ResponseEntity<ChatMessageDTO> sendMessage(
            @RequestBody SendMessageDTO messageDto,
            @AuthenticationPrincipal Jwt jwt) {

        try {
            String keycloakId = jwt.getSubject();
            Long userId = chatService.getUserIdByKeycloakId(keycloakId);

            // Validation
            if (messageDto.getContent() == null || messageDto.getContent().trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            if (messageDto.getRoomId() == null) {
                return ResponseEntity.badRequest().build();
            }

            ChatMessageDTO sentMessage = chatService.sendMessage(messageDto, userId);
            return ResponseEntity.ok(sentMessage);
        } catch (Exception e) {
            log.error("Error sending message: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Get room messages")
    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<Page<ChatMessageDTO>> getRoomMessages(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication) {
        Long userId = extractUserIdFromAuth(authentication);
        Pageable pageable = PageRequest.of(page, size);
        Page<ChatMessageDTO> messages = chatService.getRoomMessages(roomId, userId, pageable);
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/messages/{messageId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long messageId,
            @RequestParam String roomId,
            @AuthenticationPrincipal Jwt jwt) {

        try {
            String keycloakId = jwt.getSubject();
            Long userId = chatService.getUserIdByKeycloakId(keycloakId);

            chatService.markAsRead(roomId, messageId, userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error marking message as read: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Search messages in room")
    @GetMapping("/rooms/{roomId}/search")
    public ResponseEntity<List<ChatMessageDTO>> searchMessages(
            @PathVariable String roomId,
            @RequestParam String query,
            Authentication authentication) {
        Long userId = extractUserIdFromAuth(authentication);
        List<ChatMessageDTO> messages = chatService.searchMessages(roomId, query, userId);
        return ResponseEntity.ok(messages);
    }

    @PutMapping("/messages/{messageId}")
    public ResponseEntity<ChatMessageDTO> editMessage(
            @PathVariable Long messageId,
            @RequestBody Map<String, String> requestBody,
            @AuthenticationPrincipal Jwt jwt) {

        try {
            String keycloakId = jwt.getSubject();
            Long userId = chatService.getUserIdByKeycloakId(keycloakId);

            String newContent = requestBody.get("content");
            if (newContent == null || newContent.trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            ChatMessageDTO updatedMessage = chatService.editMessage(messageId, newContent, userId);
            return ResponseEntity.ok(updatedMessage);
        } catch (Exception e) {
            log.error("Error editing message: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Delete message")
    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable Long messageId,
            Authentication authentication) {
        Long userId = extractUserIdFromAuth(authentication);
        chatService.deleteMessageWithFile(messageId, userId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Add participants to group")
    @PostMapping("/rooms/{roomId}/participants")
    public ResponseEntity<Void> addParticipants(
            @PathVariable String roomId,
            @RequestBody List<Long> userIds,
            Authentication authentication) {
        Long userId = extractUserIdFromAuth(authentication);
        chatService.addParticipants(roomId, userIds, userId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Remove participant from group")
    @DeleteMapping("/rooms/{roomId}/participants/{participantId}")
    public ResponseEntity<Void> removeParticipant(
            @PathVariable String roomId,
            @PathVariable Long participantId,
            Authentication authentication) {
        Long userId = extractUserIdFromAuth(authentication);
        chatService.removeParticipant(roomId, participantId, userId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Leave group")
    @PostMapping("/rooms/{roomId}/leave")
    public ResponseEntity<Void> leaveGroup(
            @PathVariable String roomId,
            Authentication authentication) {
        Long userId = extractUserIdFromAuth(authentication);
        chatService.leaveGroup(roomId, userId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Update group info")
    @PutMapping("/rooms/{roomId}")
    public ResponseEntity<ChatRoomDTO> updateGroupInfo(
            @PathVariable String roomId,
            @RequestBody CreateGroupDTO updateDTO,
            Authentication authentication) {
        Long userId = extractUserIdFromAuth(authentication);
        ChatRoomDTO chatRoom = chatService.updateGroupInfo(roomId, updateDTO, userId);
        return ResponseEntity.ok(chatRoom);
    }

    @Operation(summary = "Get active participants of a chat room")
    @GetMapping("/rooms/{roomId}/participants")
    public ResponseEntity<List<ChatParticipantDTO>> getActiveParticipants(
            @PathVariable String roomId,
            Authentication authentication) {

        Long userId = extractUserIdFromAuth(authentication);

        if (!chatService.isUserParticipant(roomId, userId)) {
            return ResponseEntity.status(403).build();
        }

        List<ChatParticipantDTO> participants = chatService.getActiveParticipants(roomId);
        return ResponseEntity.ok(participants);
    }

    @GetMapping("/debug/{roomId}")
    public ResponseEntity<String> debugParticipation(
            @PathVariable String roomId,
            Authentication authentication) {
        Long userId = extractUserIdFromAuth(authentication);
        chatService.debugParticipation(roomId, userId);
        return ResponseEntity.ok("Check console logs");
    }

    @Operation(summary = "Get user ID from Keycloak ID")
    @GetMapping("/user-id/{keycloakId}")
    public ResponseEntity<Long> getUserIdFromKeycloakId(@PathVariable String keycloakId) {
        Long userId = chatService.getUserIdByKeycloakId(keycloakId);
        return ResponseEntity.ok(userId);
    }

    // ✅ ENDPOINT PRINCIPAL POUR L'UPLOAD DE FICHIERS
    @PostMapping("/rooms/{roomId}/files")
    @Operation(
            summary = "Upload et envoyer un fichier dans le chat",
            description = "Upload un fichier (image, vidéo, audio, document) vers Cloudinary et l'envoie dans la room de chat",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Fichier envoyé avec succès"),
                    @ApiResponse(responseCode = "400", description = "Fichier invalide ou manquant"),
                    @ApiResponse(responseCode = "404", description = "Room de chat non trouvée"),
                    @ApiResponse(responseCode = "403", description = "Utilisateur non autorisé dans cette room"),
                    @ApiResponse(responseCode = "413", description = "Fichier trop volumineux"),
                    @ApiResponse(responseCode = "500", description = "Erreur lors de l'upload Cloudinary")
            }
    )
    public ChatMessageDTO uploadFile(
            @Parameter(description = "ID de la room de chat", required = true)
            @PathVariable String roomId,

            @Parameter(description = "Fichier à envoyer (max 10MB)", required = true)
            @RequestParam("file") MultipartFile file,

            Authentication authentication
    ) {
        try {
            // Validation basique du fichier
            if (file == null || file.isEmpty()) {
                throw new RuntimeException("Le fichier est obligatoire et ne peut pas être vide");
            }

            // Validation de la taille (10MB max)
            if (file.getSize() > 10 * 1024 * 1024) {
                throw new RuntimeException("Le fichier est trop volumineux (maximum 10MB autorisé)");
            }

            // Validation optionnelle du type de fichier
            String contentType = file.getContentType();
            if (contentType == null || !isAllowedFileType(contentType)) {
                throw new RuntimeException("Type de fichier non autorisé");
            }

            // Extraction de l'ID utilisateur (même méthode que dans debugParticipation)
            Long userId = extractUserIdFromAuth(authentication);

            log.info("Upload fichier: {} ({} bytes, {}) dans room {} par user {}",
                    file.getOriginalFilename(), file.getSize(), contentType, roomId, userId);

            // Appel du service avec Cloudinary
            return chatService.sendFileMessage(roomId, file, userId);

        } catch (RuntimeException e) {
            log.error("Erreur upload fichier room {}: {}", roomId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Erreur inattendue upload fichier room {}: {}", roomId, e.getMessage());
            throw new RuntimeException("Erreur lors de l'upload du fichier: " + e.getMessage());
        }
    }

    /**
     * Validation des types de fichiers autorisés
     */
    private boolean isAllowedFileType(String contentType) {
        return contentType.startsWith("image/") ||
                contentType.startsWith("video/") ||
                contentType.startsWith("audio/") ||
                contentType.equals("application/pdf") ||
                contentType.equals("application/msword") ||
                contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") ||
                contentType.equals("application/vnd.ms-excel") ||
                contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") ||
                contentType.equals("text/plain") ||
                contentType.equals("application/zip") ||
                contentType.equals("application/x-rar-compressed");
    }

    /**
     * Extrait l'ID utilisateur depuis l'authentication
     * MÊME MÉTHODE que dans le reste du controller
     */
    private Long extractUserIdFromAuth(Authentication authentication) {
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            // Récupérer l'ID utilisateur depuis le token JWT
            String keycloakId = jwt.getSubject();
            // Convertir keycloakId en userId
            return chatService.getUserIdByKeycloakId(keycloakId);
        }
        throw new RuntimeException("Unable to extract user ID from authentication");
    }
}