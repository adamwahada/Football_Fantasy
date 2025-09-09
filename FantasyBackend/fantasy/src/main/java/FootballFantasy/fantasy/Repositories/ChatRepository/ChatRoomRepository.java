package FootballFantasy.fantasy.Repositories.ChatRepository;

import FootballFantasy.fantasy.Entities.Chat.ChatRoom;
import FootballFantasy.fantasy.Entities.Chat.ChatRoomType;
import FootballFantasy.fantasy.Entities.Chat.SupportType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByRoomId(String roomId);

    @Query("SELECT cr FROM ChatRoom cr JOIN cr.participants cp WHERE cp.user.id = :userId AND cp.isActive = true ORDER BY cr.lastActivity DESC")
    List<ChatRoom> findUserChatRooms(@Param("userId") Long userId);

    @Query("SELECT cr FROM ChatRoom cr JOIN cr.participants cp1 JOIN cr.participants cp2 " +
            "WHERE cp1.user.id = :userId1 AND cp2.user.id = :userId2 AND cr.type = :type")
    Optional<ChatRoom> findPrivateChatRoom(@Param("userId1") Long userId1,
                                           @Param("userId2") Long userId2,
                                           @Param("type") ChatRoomType type);

    List<ChatRoom> findByTypeOrderByLastActivityDesc(ChatRoomType type);

    // AJOUTER ces méthodes à ton ChatRoomRepository existant :

    @Query("SELECT cr FROM ChatRoom cr JOIN cr.participants p " +
            "WHERE p.user.id = :userId AND cr.isSupportChat = true AND cr.supportStatus != 'CLOSED' " +
            "ORDER BY cr.lastActivity DESC")
    List<ChatRoom> findUserSupportTickets(@Param("userId") Long userId);

    @Query("SELECT cr FROM ChatRoom cr JOIN cr.participants p " +
            "WHERE p.user.id = :adminId AND cr.isSupportChat = true " +
            "ORDER BY cr.lastActivity DESC")
    List<ChatRoom> findAdminSupportTickets(@Param("adminId") Long adminId);

    @Query("SELECT cr FROM ChatRoom cr JOIN cr.participants p " +
            "WHERE p.user.id = :userId AND p.isActive = true " +
            "ORDER BY cr.lastActivity DESC")
    List<ChatRoom> findAdminChats(@Param("userId") Long userId);

    // ✅ REQUÊTE CORRIGÉE - Solution simple et propre
    @Query("SELECT cr FROM ChatRoom cr JOIN cr.participants p " +
            "WHERE p.user.id = :userId AND cr.supportType = :supportType " +
            "AND cr.supportStatus != 'RESOLVED' AND cr.supportStatus != 'CLOSED'")
    Optional<ChatRoom> findActiveSupportTicket(@Param("userId") Long userId,
                                               @Param("supportType") SupportType supportType);

    @Query("SELECT c FROM ChatRoom c WHERE c.type = 'SUPPORT' ORDER BY c.createdAt DESC")
    List<ChatRoom> findSupportChatsOrderByCreatedDesc();




}