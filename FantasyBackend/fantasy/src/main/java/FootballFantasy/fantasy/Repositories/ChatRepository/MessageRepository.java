package FootballFantasy.fantasy.Repositories.ChatRepository;

import FootballFantasy.fantasy.Entities.Chat.Message;
import FootballFantasy.fantasy.Entities.UserEntity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m WHERE " +
            "(m.sender = :user1 AND m.receiver = :user2) OR " +
            "(m.sender = :user2 AND m.receiver = :user1) " +
            "ORDER BY m.sentAt ASC")
    List<Message> findMessagesBetweenUsers(@Param("user1") UserEntity user1,
                                           @Param("user2") UserEntity user2);

    @Query("SELECT m FROM Message m WHERE m.receiver = :user AND m.isRead = false")
    List<Message> findUnreadMessages(@Param("user") UserEntity user);

    @Query("SELECT DISTINCT CASE " +
            "WHEN m.sender = :user THEN m.receiver " +
            "ELSE m.sender END " +
            "FROM Message m WHERE m.sender = :user OR m.receiver = :user")
    List<UserEntity> findChatPartners(@Param("user") UserEntity user);

    @Query("SELECT m FROM Message m WHERE " +
            "(m.sender = :user1 AND m.receiver = :user2) OR " +
            "(m.sender = :user2 AND m.receiver = :user1) " +
            "ORDER BY m.sentAt DESC")
    List<Message> findLastMessageBetweenUsers(@Param("user1") UserEntity user1,
                                              @Param("user2") UserEntity user2);
}
