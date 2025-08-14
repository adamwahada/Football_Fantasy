package FootballFantasy.fantasy.Controller.ControllerUser;

import FootballFantasy.fantasy.Entities.UserEntity.UserEntity;
import FootballFantasy.fantasy.Services.UserService.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * Get current user profile
     */
    @GetMapping("/profile")
    public ResponseEntity<UserEntity> getCurrentUserProfile() {
        try {
            UserEntity user = userService.ensureCurrentUserFromToken();
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update current user profile
     */
    @PutMapping("/profile")
    public ResponseEntity<UserEntity> updateProfile(@RequestBody UserService.UserProfileUpdateRequest request) {
        try {
            UserEntity updatedUser = userService.updateCurrentUserProfile(request);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Check if username is available
     */
    @GetMapping("/check-username/{username}")
    public ResponseEntity<AvailabilityResponse> checkUsername(@PathVariable String username) {
        try {
            boolean available = userService.isUsernameAvailable(username);
            return ResponseEntity.ok(new AvailabilityResponse(available));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Check if email is available
     */
    @GetMapping("/check-email/{email}")
    public ResponseEntity<AvailabilityResponse> checkEmail(@PathVariable String email) {
        try {
            boolean available = userService.isEmailAvailable(email);
            return ResponseEntity.ok(new AvailabilityResponse(available));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get current user statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<UserService.UserStatsResponse> getUserStats() {
        try {
            UserService.UserStatsResponse stats = userService.getCurrentUserStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete current user account
     */
    @DeleteMapping("/profile")
    public ResponseEntity<Void> deleteAccount() {
        try {
            userService.deleteUser(userService.getCurrentAppUserId());
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ===== INNER CLASSES =====
    public static class AvailabilityResponse {
        private final boolean available;

        public AvailabilityResponse(boolean available) {
            this.available = available;
        }

        public boolean isAvailable() {
            return available;
        }
    }

    @PostMapping("/auto-create")
    public ResponseEntity<UserEntity> autoCreateUser() {
        try {
            // Ensures the current user exists in your app DB
            UserEntity user = userService.ensureCurrentUserFromToken();
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody UserService.UserProfileUpdateRequest request) {
        try {
            // Get Keycloak ID of currently authenticated user
            Long currentUserId;
            String keycloakId;
            try {
                currentUserId = userService.getCurrentAppUserId();
                UserEntity existingUser = userService.getCurrentUser();
                keycloakId = existingUser.getKeycloakId();
            } catch (Exception e) {
                // If user does not exist yet, get Keycloak ID from token
                keycloakId = userService.ensureCurrentUserFromToken().getKeycloakId();
            }

            // Create or update user with Keycloak ID and request data
            UserEntity user = userService.createOrUpdateUser(
                    keycloakId,
                    request.getFirstName(),    // username
                    request.getEmail(),        // email
                    request.getFirstName(),    // firstName
                    request.getLastName(),     // lastName
                    request.getPhone(),
                    request.getCountry(),
                    request.getAddress(),
                    request.getPostalNumber(),
                    request.getBirthDate()
            );

            return ResponseEntity.ok(Map.of(
                    "message", "User created/updated successfully",
                    "userId", user.getId()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

}
