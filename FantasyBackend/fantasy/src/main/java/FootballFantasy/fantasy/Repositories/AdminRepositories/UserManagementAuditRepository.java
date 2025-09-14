package FootballFantasy.fantasy.Repositories.AdminRepositories;

import FootballFantasy.fantasy.Entities.AdminEntities.BanCause;
import FootballFantasy.fantasy.Entities.AdminEntities.UserManagementAudit;
import FootballFantasy.fantasy.Entities.AdminEntities.UserAction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserManagementAuditRepository extends JpaRepository<UserManagementAudit, Long> {

    // Fetch all for a specific user
    List<UserManagementAudit> findByUserId(Long userId);

    // Fetch all performed by a specific admin
    List<UserManagementAudit> findByAdminId(Long adminId);

    // Fetch all audit entries of a specific type/action for a user
    List<UserManagementAudit> findByUserIdAndAction(Long userId, UserAction action);

    // Fetch all actions of a specific type performed by an admin
    List<UserManagementAudit> findByAdminIdAndAction(Long adminId, UserAction action);

    List<UserManagementAudit> findByReason(BanCause reason);

    List<UserManagementAudit> findByUserIdAndReason(Long userId, BanCause reason);

}
