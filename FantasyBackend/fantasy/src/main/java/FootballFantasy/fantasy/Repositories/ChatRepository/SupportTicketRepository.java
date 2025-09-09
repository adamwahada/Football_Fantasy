package FootballFantasy.fantasy.Repositories.ChatRepository;

import FootballFantasy.fantasy.Entities.Chat.SupportStatus;
import FootballFantasy.fantasy.Entities.Chat.SupportTicket;
import FootballFantasy.fantasy.Entities.Chat.SupportType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    // Trouver un ticket par son ticketId unique
    Optional<SupportTicket> findByTicketId(String ticketId);

    // Trouver tous les tickets d'un utilisateur
    @Query("SELECT t FROM SupportTicket t WHERE t.user.id = :userId ORDER BY t.createdAt DESC")
    List<SupportTicket> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    // Trouver les tickets actifs d'un utilisateur (OPEN ou IN_PROGRESS)
    @Query("SELECT t FROM SupportTicket t WHERE t.user.id = :userId AND t.status IN ('OPEN', 'IN_PROGRESS') ORDER BY t.createdAt DESC")
    List<SupportTicket> findActiveTicketsByUserId(@Param("userId") Long userId);

    // Vérifier si un utilisateur a déjà un ticket ouvert du même type
    @Query("SELECT t FROM SupportTicket t WHERE t.user.id = :userId AND t.supportType = :supportType AND t.status IN ('OPEN', 'IN_PROGRESS')")
    Optional<SupportTicket> findActiveTicketByUserIdAndType(@Param("userId") Long userId, @Param("supportType") SupportType supportType);

    // Tous les tickets pour l'admin, triés par priorité et date
    @Query("SELECT t FROM SupportTicket t ORDER BY " +
            "CASE t.priority " +
            "  WHEN 'URGENT' THEN 1 " +
            "  WHEN 'HIGH' THEN 2 " +
            "  WHEN 'MEDIUM' THEN 3 " +
            "  WHEN 'LOW' THEN 4 " +
            "END, t.createdAt DESC")
    List<SupportTicket> findAllOrderByPriorityAndDate();

    // Tickets assignés à un admin spécifique
    @Query("SELECT t FROM SupportTicket t WHERE t.assignedAdmin.id = :adminId ORDER BY t.updatedAt DESC")
    List<SupportTicket> findByAssignedAdminId(@Param("adminId") Long adminId);

    // Tickets par statut
    List<SupportTicket> findByStatusOrderByCreatedAtDesc(SupportStatus status);

    // Tickets par type de support
    List<SupportTicket> findBySupportTypeOrderByCreatedAtDesc(SupportType supportType);

    // Compter les tickets par statut
    long countByStatus(SupportStatus status);

    // Compter les tickets créés dans une période
    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.createdAt >= :startDate AND t.createdAt <= :endDate")
    long countByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // Statistiques pour le dashboard admin
    @Query("SELECT " +
            "SUM(CASE WHEN t.status = 'OPEN' THEN 1 ELSE 0 END) as openTickets, " +
            "SUM(CASE WHEN t.status = 'IN_PROGRESS' THEN 1 ELSE 0 END) as inProgressTickets, " +
            "SUM(CASE WHEN t.status = 'RESOLVED' THEN 1 ELSE 0 END) as resolvedTickets, " +
            "SUM(CASE WHEN t.status = 'CLOSED' THEN 1 ELSE 0 END) as closedTickets " +
            "FROM SupportTicket t")
    Object[] getTicketStatistics();

    // Tickets récents (dernières 24h)
    @Query("SELECT t FROM SupportTicket t WHERE t.createdAt >= :since ORDER BY t.createdAt DESC")
    List<SupportTicket> findRecentTickets(@Param("since") LocalDateTime since);

    // Tickets non assignés (pour auto-assignment)
    @Query("SELECT t FROM SupportTicket t WHERE t.assignedAdmin IS NULL AND t.status = 'OPEN' ORDER BY t.createdAt ASC")
    List<SupportTicket> findUnassignedTickets();

    // Recherche dans les tickets
    @Query("SELECT t FROM SupportTicket t WHERE " +
            "LOWER(t.subject) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(t.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "t.ticketId LIKE CONCAT('%', :query, '%') " +
            "ORDER BY t.createdAt DESC")
    List<SupportTicket> searchTickets(@Param("query") String query);

    // Tickets par ChatRoom ID (pour la liaison)
    @Query("SELECT t FROM SupportTicket t WHERE t.chatRoom.id = :chatRoomId")
    Optional<SupportTicket> findByChatRoomId(@Param("chatRoomId") Long chatRoomId);

    // Tickets par ChatRoom roomId (UUID)
    @Query("SELECT t FROM SupportTicket t WHERE t.chatRoom.roomId = :roomId")
    Optional<SupportTicket> findByChatRoomRoomId(@Param("roomId") String roomId);

    // Compter les tickets assignés à un admin spécifique
    @Query("SELECT COUNT(t) FROM SupportTicket t WHERE t.assignedAdmin.id = :adminId")
    long countByAssignedAdminId(@Param("adminId") Long adminId);
}