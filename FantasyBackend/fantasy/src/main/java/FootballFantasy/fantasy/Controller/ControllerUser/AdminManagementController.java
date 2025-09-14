package FootballFantasy.fantasy.Controller.ControllerUser;

import FootballFantasy.fantasy.Dto.UserManagementAuditDTO;
import FootballFantasy.fantasy.Entities.AdminEntities.BanCause;
import FootballFantasy.fantasy.Entities.AdminEntities.UserAction;
import FootballFantasy.fantasy.Entities.AdminEntities.UserManagementAudit;
import FootballFantasy.fantasy.Repositories.AdminRepositories.UserManagementAuditRepository;
import FootballFantasy.fantasy.Repositories.UserRepositories.UserRepository;
import FootballFantasy.fantasy.Services.UserService.AdminManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin-management")
public class AdminManagementController {

    @Autowired
    private AdminManagementService adminService;
    @Autowired
    private UserManagementAuditRepository auditRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/audit/all")
    public ResponseEntity<List<UserManagementAuditDTO>> getAllAuditRecords() {
        List<UserManagementAudit> audits = auditRepository.findAll();
        List<UserManagementAuditDTO> auditDTOs = audits.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(auditDTOs);
    }

    private UserManagementAuditDTO convertToDTO(UserManagementAudit audit) {
        UserManagementAuditDTO dto = new UserManagementAuditDTO();
        dto.setId(audit.getId());
        dto.setUserId(audit.getUserId());
        dto.setAdminId(audit.getAdminId());
        dto.setAction(audit.getAction());
        dto.setReason(audit.getReason());
        dto.setDetails(audit.getDetails());
        dto.setTimestamp(audit.getTimestamp());

        // Format date for frontend
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        dto.setFormattedDate(audit.getTimestamp().format(formatter));

        // Get real username and email for the user
        userRepository.findById(audit.getUserId()).ifPresent(user -> {
            dto.setUsername(user.getUsername());
            dto.setEmail(user.getEmail());
        });

        // Get real username for the admin (assuming admin is also a user in the system)
        userRepository.findById(audit.getAdminId()).ifPresent(admin -> {
            dto.setAdminUsername(admin.getUsername());
        });

        // Extract amount and days from details if needed
        if (audit.getDetails() != null) {
            // Parse details string for amount/days if stored there
            // Example: "Amount: 50.0" or "Duration: 7 days"
            if (audit.getDetails().contains("Amount:")) {
                try {
                    String amountStr = audit.getDetails().replaceAll("Amount: ", "").replaceAll("[^0-9.]", "");
                    dto.setAmount(Double.parseDouble(amountStr));
                } catch (NumberFormatException ignored) {}
            }
            if (audit.getDetails().contains("Duration:")) {
                try {
                    String daysStr = audit.getDetails().replaceAll("Duration: ", "").replaceAll("[^0-9]", "");
                    dto.setDays(Integer.parseInt(daysStr));
                } catch (NumberFormatException ignored) {}
            }
        }

        return dto;
    }
    // --- User history endpoints ---

    // Get full history of a user
    @GetMapping("/users/{userId}/history")
    public List<UserManagementAudit> getUserHistory(@PathVariable Long userId) {
        return adminService.getUserHistory(userId);
    }

    // Get history of a user filtered by action
    @GetMapping("/users/{userId}/history/action/{action}")
    public List<UserManagementAudit> getUserHistoryByAction(
            @PathVariable Long userId,
            @PathVariable UserAction action) {
        return adminService.getUserHistoryByAction(userId, action);
    }

    // Get history of a user filtered by reason
    @GetMapping("/users/{userId}/history/reason/{reason}")
    public List<UserManagementAudit> getUserHistoryByReason(
            @PathVariable Long userId,
            @PathVariable BanCause reason) {
        return adminService.getUserHistoryByReason(userId, reason);
    }

    // --- Admin action endpoints ---

    // Get all actions performed by an admin
    @GetMapping("/admins/{adminId}/actions")
    public List<UserManagementAudit> getAdminActions(@PathVariable Long adminId) {
        return adminService.getAdminActions(adminId);
    }

    // Get all actions performed by an admin filtered by action
    @GetMapping("/admins/{adminId}/actions/{action}")
    public List<UserManagementAudit> getAdminHistoryByAction(
            @PathVariable Long adminId,
            @PathVariable UserAction action) {
        return adminService.getAdminHistoryByAction(adminId, action);
    }

    // --- Actions by reason endpoint ---

    // Get all actions in the system with a specific reason
    @GetMapping("/actions/reason/{reason}")
    public List<UserManagementAudit> getActionsByReason(@PathVariable BanCause reason) {
        return adminService.getActionsByReason(reason);
    }
}
