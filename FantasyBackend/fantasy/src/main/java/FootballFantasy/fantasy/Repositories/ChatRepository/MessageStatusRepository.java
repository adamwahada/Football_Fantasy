package FootballFantasy.fantasy.Repositories.ChatRepository;

import FootballFantasy.fantasy.Entities.Chat.MessageStatus;
import FootballFantasy.fantasy.Entities.Chat.MessageStatusType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageStatusRepository extends JpaRepository<MessageStatus, Long> {

    Optional<MessageStatus> findByMessageIdAndUserId(Long messageId, Long userId);

    @Query("SELECT ms FROM MessageStatus ms WHERE ms.message.chatRoom.id = :chatRoomId AND ms.user.id = :userId")
    List<MessageStatus> findByUserAndChatRoom(@Param("userId") Long userId, @Param("chatRoomId") Long chatRoomId);

    @Query("SELECT COUNT(ms) FROM MessageStatus ms WHERE ms.message.chatRoom.id = :chatRoomId AND ms.user.id = :userId AND ms.status = :status")
    Long countByStatusAndUserAndChatRoom(@Param("chatRoomId") Long chatRoomId,
                                         @Param("userId") Long userId,
                                         @Param("status") MessageStatusType status);
}