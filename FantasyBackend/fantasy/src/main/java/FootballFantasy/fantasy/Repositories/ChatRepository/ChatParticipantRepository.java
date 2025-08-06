package FootballFantasy.fantasy.Repositories.ChatRepository;

import FootballFantasy.fantasy.Entities.Chat.ChatParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {

    Optional<ChatParticipant> findByChatRoomIdAndUserId(Long chatRoomId, Long userId);

    List<ChatParticipant> findByChatRoomIdAndIsActiveTrue(Long chatRoomId);

    @Query("SELECT cp FROM ChatParticipant cp WHERE cp.user.id = :userId AND cp.isActive = true")
    List<ChatParticipant> findUserActiveParticipations(@Param("userId") Long userId);

    boolean existsByChatRoomIdAndUserIdAndIsActiveTrue(Long chatRoomId, Long userId);


    @Query("SELECT cp FROM ChatParticipant cp " +
            "WHERE cp.chatRoom.roomId = :roomId AND cp.user.id = :userId")
    Optional<ChatParticipant> findByRoomIdAndUserId(@Param("roomId") String roomId, @Param("userId") Long userId);


    @Query("SELECT CASE WHEN COUNT(cp) > 0 THEN true ELSE false END " +
            "FROM ChatParticipant cp " +
            "WHERE cp.chatRoom.roomId = :roomId AND cp.user.id = :userId AND cp.isActive = true")
    boolean existsByRoomIdAndUserIdAndIsActiveTrue(@Param("roomId") String roomId, @Param("userId") Long userId);



}