package FootballFantasy.fantasy.Controller.ChatController;

import FootballFantasy.fantasy.Dto.ChatDto.*;
import FootballFantasy.fantasy.Services.ChatService.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
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
            @Valid @RequestBody SendMessageDTO messageDTO,
            Authentication authentication) {
        Long userId = extractUserIdFromAuth(authentication);
        ChatMessageDTO message = chatService.sendMessage(messageDTO, userId);
        return ResponseEntity.ok(message);
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

    @Operation(summary = "Mark message as read")
    @PostMapping("/messages/{messageId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long messageId,
            @RequestParam String roomId,
            Authentication authentication) {
        Long userId = extractUserIdFromAuth(authentication);
        chatService.markAsRead(roomId, messageId, userId);
        return ResponseEntity.ok().build();
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

    @Operation(summary = "Edit message")
    @PutMapping("/messages/{messageId}")
    public ResponseEntity<ChatMessageDTO> editMessage(
            @PathVariable Long messageId,
            @RequestBody String newContent,
            Authentication authentication) {
        Long userId = extractUserIdFromAuth(authentication);
        ChatMessageDTO message = chatService.editMessage(messageId, newContent, userId);
        return ResponseEntity.ok(message);
    }

    @Operation(summary = "Delete message")
    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable Long messageId,
            Authentication authentication) {
        Long userId = extractUserIdFromAuth(authentication);
        chatService.deleteMessage(messageId, userId);
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

    private Long extractUserIdFromAuth(Authentication authentication) {
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            // Récupérer l'ID utilisateur depuis le token JWT
            String keycloakId = jwt.getSubject();
            // Vous devez implémenter une méthode pour convertir keycloakId en userId
            return chatService.getUserIdByKeycloakId(keycloakId);
        }
        throw new RuntimeException("Unable to extract user ID from authentication");
    }









    @Operation(summary = "Get active participants of a chat room")
    @GetMapping("/rooms/{roomId}/participants")
    public ResponseEntity<List<ChatParticipantDTO>> getActiveParticipants(
            @PathVariable String roomId,
            Authentication authentication) {

        Long userId = extractUserIdFromAuth(authentication);

        // Optionnel : vérifier que l'utilisateur est participant pour sécurité
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
}