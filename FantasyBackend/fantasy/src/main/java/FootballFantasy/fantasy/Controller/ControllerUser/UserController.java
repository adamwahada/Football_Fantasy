package FootballFantasy.fantasy.Controller.ControllerUser;

import FootballFantasy.fantasy.Entities.UserEntity.UserEntity;
import FootballFantasy.fantasy.Services.UserService.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * Get current user profile (from Keycloak token)
     */
    @GetMapping("/profile")
    public ResponseEntity<UserEntity> getCurrentUserProfile(@AuthenticationPrincipal Jwt jwt) {
        try {
            String keycloakId = jwt.getSubject();
            Optional<UserEntity> user = userService.getUserByKeycloakId(keycloakId);

            if (user.isPresent()) {
                return ResponseEntity.ok(user.get());
            } else {
                // Auto-create user from Keycloak token
                UserEntity newUser = userService.createOrUpdateUser(
                        keycloakId,
                        jwt.getClaimAsString("preferred_username"),
                        jwt.getClaimAsString("email"),
                        jwt.getClaimAsString("given_name"),
                        jwt.getClaimAsString("family_name"),
                        jwt.getClaimAsString("phone"),
                        jwt.getClaimAsString("country"),
                        jwt.getClaimAsString("address"),
                        jwt.getClaimAsString("postalNumber"),
                        parseBirthDate(jwt.getClaimAsString("birthDate"))

                );
                return ResponseEntity.ok(newUser);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get user by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserEntity> getUserById(@PathVariable Long id) {
        try {
            Optional<UserEntity> user = userService.getUserById(id);
            return user.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get user by username
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<UserEntity> getUserByUsername(@PathVariable String username) {
        try {
            Optional<UserEntity> user = userService.getUserByUsername(username);
            return user.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update current user profile
     */
    @PutMapping("/profile")
    public ResponseEntity<UserEntity> updateProfile(@AuthenticationPrincipal Jwt jwt,
                                                    @RequestBody UserService.UserProfileUpdateRequest request) {
        try {
            String keycloakId = jwt.getSubject();
            Optional<UserEntity> userOpt = userService.getUserByKeycloakId(keycloakId);

            if (userOpt.isPresent()) {
                UserEntity updatedUser = userService.updateUserProfile(userOpt.get().getId(), request);
                return ResponseEntity.ok(updatedUser);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Accept terms and conditions
     */
    @PostMapping("/accept-terms")
    public ResponseEntity<UserEntity> acceptTerms(@AuthenticationPrincipal Jwt jwt) {
        try {
            String keycloakId = jwt.getSubject();
            Optional<UserEntity> userOpt = userService.getUserByKeycloakId(keycloakId);

            if (userOpt.isPresent()) {
                UserEntity updatedUser = userService.acceptTerms(userOpt.get().getId());
                return ResponseEntity.ok(updatedUser);
            } else {
                return ResponseEntity.notFound().build();
            }
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
    public ResponseEntity<UserService.UserStatsResponse> getUserStats(@AuthenticationPrincipal Jwt jwt) {
        try {
            String keycloakId = jwt.getSubject();
            Optional<UserEntity> userOpt = userService.getUserByKeycloakId(keycloakId);

            if (userOpt.isPresent()) {
                UserService.UserStatsResponse stats = userService.getUserStats(userOpt.get().getId());
                return ResponseEntity.ok(stats);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get user statistics by ID (admin or public)
     */
    @GetMapping("/{id}/stats")
    public ResponseEntity<UserService.UserStatsResponse> getUserStatsById(@PathVariable Long id) {
        try {
            UserService.UserStatsResponse stats = userService.getUserStats(id);
            return ResponseEntity.ok(stats);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all users (admin only)
     */
    @GetMapping("/all")
    public ResponseEntity<List<UserEntity>> getAllUsers() {
        try {
            // TODO: Add admin role check
            List<UserEntity> users = userService.getAllUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete current user account
     */
    @DeleteMapping("/profile")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal Jwt jwt) {
        try {
            String keycloakId = jwt.getSubject();
            Optional<UserEntity> userOpt = userService.getUserByKeycloakId(keycloakId);

            if (userOpt.isPresent()) {
                userService.deleteUser(userOpt.get().getId());
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create or update user (used for Keycloak integration)
     */
    @PostMapping("/sync")
    public ResponseEntity<UserEntity> syncUser(@AuthenticationPrincipal Jwt jwt) {
        try {
            String keycloakId = jwt.getSubject();

            UserEntity newUser = userService.createOrUpdateUser(
                    keycloakId,
                    jwt.getClaimAsString("preferred_username"),
                    jwt.getClaimAsString("email"),
                    jwt.getClaimAsString("given_name"),
                    jwt.getClaimAsString("family_name"),
                    jwt.getClaimAsString("phone"),
                    jwt.getClaimAsString("country"),
                    jwt.getClaimAsString("address"),
                    jwt.getClaimAsString("postalNumber"),
                    parseBirthDate(jwt.getClaimAsString("birthDate"))
            );
            return ResponseEntity.ok(newUser);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ===== INNER CLASSES =====

    /**
     * Availability response for username/email checks
     */
    public static class AvailabilityResponse {
        private final boolean available;

        public AvailabilityResponse(boolean available) {
            this.available = available;
        }

        public boolean isAvailable() {
            return available;
        }
    }
    private LocalDate parseBirthDate(String birthDateString) {
        try {
            return birthDateString != null ? LocalDate.parse(birthDateString) : null;
        } catch (Exception e) {
            return null; // en cas de format invalide
        }
    }
}