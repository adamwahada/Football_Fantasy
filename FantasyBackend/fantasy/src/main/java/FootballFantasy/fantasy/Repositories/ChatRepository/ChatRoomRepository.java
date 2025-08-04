package FootballFantasy.fantasy.Repositories.ChatRepository;

import FootballFantasy.fantasy.Entities.Chat.ChatRoom;
import FootballFantasy.fantasy.Entities.Chat.ChatRoomType;
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
}
