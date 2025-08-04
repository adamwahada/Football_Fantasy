package FootballFantasy.fantasy.Repositories.ChatRepository;

import FootballFantasy.fantasy.Entities.Chat.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Page<ChatMessage> findByChatRoomIdAndIsDeletedFalseOrderByTimestampDesc(Long chatRoomId, Pageable pageable);

    @Query("SELECT cm FROM ChatMessage cm WHERE cm.chatRoom.id = :chatRoomId AND cm.timestamp > :since AND (cm.isDeleted = false OR cm.isDeleted IS NULL) ORDER BY cm.timestamp ASC")
    List<ChatMessage> findNewMessages(@Param("chatRoomId") Long chatRoomId, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(cm) FROM ChatMessage cm JOIN MessageStatus ms ON cm.id = ms.message.id " +
            "WHERE cm.chatRoom.id = :chatRoomId AND ms.user.id = :userId AND ms.status != 'READ' AND cm.sender.id != :userId")
    Long countUnreadMessages(@Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId);

    List<ChatMessage> findByChatRoomIdAndContentContainingIgnoreCaseAndIsDeletedFalse(Long chatRoomId, String content);
}