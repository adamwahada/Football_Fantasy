package FootballFantasy.fantasy.Controller.ControllerUser;

import FootballFantasy.fantasy.Entities.UserEntity.UserEntity;
import FootballFantasy.fantasy.Services.UserService.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    // ✅ Get current user profile (with balance)
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser() {
        try {
            UserEntity currentUser = userService.getCurrentUser();
            UserProfileResponse response = new UserProfileResponse(
                    currentUser.getId(),
                    currentUser.getUsername(),
                    currentUser.getEmail(),
                    currentUser.getFirstName(),
                    currentUser.getLastName(),
                    currentUser.getBalance(),
                    currentUser.getTermsAccepted()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ✅ Get just the balance (useful for quick competition checks)
    @GetMapping("/user-balance")
    public ResponseEntity<BalanceResponse> getCurrentUserBalance() {
        try {
            UserEntity currentUser = userService.getCurrentUser();
            return ResponseEntity.ok(new BalanceResponse(currentUser.getBalance()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Existing endpoints...
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

    @PutMapping("/profile")
    public ResponseEntity<UserEntity> updateProfile(@RequestBody UserService.UserProfileUpdateRequest request) {
        try {
            UserEntity updatedUser = userService.updateCurrentUserProfile(request);
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/check-username/{username}")
    public ResponseEntity<AvailabilityResponse> checkUsername(@PathVariable String username) {
        try {
            boolean available = userService.isUsernameAvailable(username);
            return ResponseEntity.ok(new AvailabilityResponse(available));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/check-email/{email}")
    public ResponseEntity<AvailabilityResponse> checkEmail(@PathVariable String email) {
        try {
            boolean available = userService.isEmailAvailable(email);
            return ResponseEntity.ok(new AvailabilityResponse(available));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<UserService.UserStatsResponse> getUserStats() {
        try {
            UserService.UserStatsResponse stats = userService.getCurrentUserStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/profile")
    public ResponseEntity<Void> deleteAccount() {
        try {
            userService.deleteUser(userService.getCurrentAppUserId());
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/auto-create")
    public ResponseEntity<UserEntity> autoCreateUser() {
        try {
            UserEntity user = userService.ensureCurrentUserFromToken();
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody UserService.UserProfileUpdateRequest request) {
        try {
            String keycloakId;
            try {
                UserEntity existingUser = userService.getCurrentUser();
                keycloakId = existingUser.getKeycloakId();
            } catch (Exception e) {
                keycloakId = userService.ensureCurrentUserFromToken().getKeycloakId();
            }

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

    public static class UserProfileResponse {
        private final Long id;
        private final String username;
        private final String email;
        private final String firstName;
        private final String lastName;
        private final BigDecimal balance;
        private final boolean termsAccepted;

        public UserProfileResponse(Long id, String username, String email, String firstName,
                                   String lastName, BigDecimal balance, boolean termsAccepted) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.firstName = firstName;
            this.lastName = lastName;
            this.balance = balance;
            this.termsAccepted = termsAccepted;
        }

        public Long getId() { return id; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public BigDecimal getBalance() { return balance; }
        public boolean isTermsAccepted() { return termsAccepted; }
    }

    public static class BalanceResponse {
        private final BigDecimal balance;
        public BalanceResponse(BigDecimal balance) {
            this.balance = balance;
        }
        public BigDecimal getBalance() { return balance; }
    }
}
