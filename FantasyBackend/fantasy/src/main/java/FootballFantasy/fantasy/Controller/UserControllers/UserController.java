package FootballFantasy.fantasy.Controller.UserControllers;

import FootballFantasy.fantasy.Entities.UserEntities.UserEntity;
import FootballFantasy.fantasy.Services.UserService.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser() {
        try {
            UserEntity currentUser = userService.ensureCurrentUserFromToken();

            UserProfileResponse response = new UserProfileResponse(
                    currentUser.getId(),
                    currentUser.getUsername(),
                    currentUser.getEmail(),
                    currentUser.getFirstName(),
                    currentUser.getLastName(),
                    currentUser.getPhone(),
                    currentUser.getCountry(),
                    currentUser.getAddress(),
                    currentUser.getPostalNumber(),
                    currentUser.getBirthDate(),
                    currentUser.getReferralCode(),
                    currentUser.getBalance(),
                    currentUser.getWithdrawableBalance(),
                    currentUser.getBonusBalance(),
                    currentUser.getPendingWithdrawals(),
                    currentUser.getPendingDeposits(),
                    currentUser.isTermsAccepted(),
                    currentUser.isActive(),
                    currentUser.getBannedUntil()
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/user-balance")
    public ResponseEntity<BalanceResponse> getCurrentUserBalance() {
        try {
            System.out.println("🔍 Getting user balance...");

            // ✅ Auto-create user if needed
            UserEntity currentUser = userService.ensureCurrentUserFromToken();

            System.out.println("✅ Balance retrieved for user: " + currentUser.getKeycloakId());

            return ResponseEntity.ok(new BalanceResponse(currentUser.getBalance()));
        } catch (Exception e) {
            System.err.println("❌ Error getting balance: " + e.getMessage());
            e.printStackTrace();
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
                    request.getUsername(),        // username
                    request.getEmail(),
                    request.getFirstName(),
                    request.getLastName(),
                    request.getPhone(),
                    request.getCountry(),
                    request.getAddress(),
                    request.getPostalNumber(),
                    request.getBirthDate(),
                    true,                         // termsAccepted
                    true,                         // active
                    BigDecimal.ZERO,              // balance
                    BigDecimal.ZERO,              // withdrawableBalance
                    BigDecimal.ZERO,              // pendingWithdrawals
                    BigDecimal.ZERO,              // pendingDeposits
                    null                          // bannedUntil
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

    // Replace your entire UserProfileResponse class with this:
    public static class UserProfileResponse {
        private final Long id;
        private final String username;
        private final String email;
        private final String firstName;
        private final String lastName;
        private final String phone;
        private final String country;
        private final String address;
        private final String postalNumber;
        private final LocalDate birthDate;
        private final String referralCode;
        private final BigDecimal balance;
        private final BigDecimal withdrawableBalance;
        private final BigDecimal bonusBalance;
        private final BigDecimal pendingWithdrawals;
        private final BigDecimal pendingDeposits;
        private final boolean termsAccepted;
        private final boolean active;
        private final LocalDateTime bannedUntil;

        public UserProfileResponse(Long id, String username, String email, String firstName,
                                   String lastName, String phone, String country, String address,
                                   String postalNumber, LocalDate birthDate, String referralCode,
                                   BigDecimal balance, BigDecimal withdrawableBalance, BigDecimal bonusBalance,
                                   BigDecimal pendingWithdrawals, BigDecimal pendingDeposits,
                                   boolean termsAccepted, boolean active, LocalDateTime bannedUntil) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.firstName = firstName;
            this.lastName = lastName;
            this.phone = phone;
            this.country = country;
            this.address = address;
            this.postalNumber = postalNumber;
            this.birthDate = birthDate;
            this.referralCode = referralCode;
            this.balance = balance;
            this.withdrawableBalance = withdrawableBalance;
            this.bonusBalance = bonusBalance;
            this.pendingWithdrawals = pendingWithdrawals;
            this.pendingDeposits = pendingDeposits;
            this.termsAccepted = termsAccepted;
            this.active = active;
            this.bannedUntil = bannedUntil;
        }

        public Long getId() { return id; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public String getFirstName() { return firstName; }
        public String getLastName() { return lastName; }
        public String getPhone() { return phone; }
        public String getCountry() { return country; }
        public String getAddress() { return address; }
        public String getPostalNumber() { return postalNumber; }
        public LocalDate getBirthDate() { return birthDate; }
        public String getReferralCode() { return referralCode; }
        public BigDecimal getBalance() { return balance; }
        public BigDecimal getWithdrawableBalance() { return withdrawableBalance; }
        public BigDecimal getBonusBalance() { return bonusBalance; }
        public BigDecimal getPendingWithdrawals() { return pendingWithdrawals; }
        public BigDecimal getPendingDeposits() { return pendingDeposits; }
        public boolean isTermsAccepted() { return termsAccepted; }
        public boolean isActive() { return active; }
        public LocalDateTime getBannedUntil() { return bannedUntil; }
    }

    public static class BalanceResponse {
        private final BigDecimal balance;
        public BalanceResponse(BigDecimal balance) {
            this.balance = balance;
        }
        public BigDecimal getBalance() { return balance; }
    }
    // ===== HELPER METHODS =====

    public String getCurrentUserKeycloakId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null) {
            return authentication.getName(); // Keycloak ID from JWT
        }
        throw new RuntimeException("User not authenticated");
    }
}
