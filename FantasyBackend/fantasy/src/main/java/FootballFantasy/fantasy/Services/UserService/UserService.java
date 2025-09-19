package FootballFantasy.fantasy.Services.UserService;

import FootballFantasy.fantasy.Entities.AdminEntities.BanCause;
import FootballFantasy.fantasy.Entities.AdminEntities.UserAction;
import FootballFantasy.fantasy.Entities.AdminEntities.UserManagementAudit;
import FootballFantasy.fantasy.Entities.UserEntities.UserEntity;
import FootballFantasy.fantasy.Exceptions.PaiementExceptions.InsufficientBalanceException;
import FootballFantasy.fantasy.Repositories.AdminRepositories.UserManagementAuditRepository;
import FootballFantasy.fantasy.Repositories.UserRepositories.UserRepository;
import FootballFantasy.fantasy.Services.GameweekService.SessionParticipationService;
import jakarta.transaction.Transactional;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import FootballFantasy.fantasy.Dto.UserSessionStats;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionParticipationService sessionParticipationService;

    @Autowired
    private UserManagementAuditRepository userManagementAuditRepository;

    @Autowired
    private KeycloakService keycloakService;

    // ======== Keycloak / Current User Helpers ========

    private Jwt getJwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt)) {
            throw new RuntimeException("User not authenticated");
        }
        return (Jwt) auth.getPrincipal();
    }

    private String getCurrentKeycloakId() {
        return getJwt().getSubject(); // Keycloak user UUID in the "sub" claim
    }

    public Long getCurrentAppUserId() {
        String keycloakId = getCurrentKeycloakId();
        return userRepository.findByKeycloakId(keycloakId)
                .map(UserEntity::getId)
                .orElseThrow(() -> new RuntimeException("App user not found for Keycloak ID: " + keycloakId));
    }
    public UserEntity ensureCurrentUserFromToken() {
        try {
            Jwt jwt = getJwt();
            String keycloakId = jwt.getSubject();

            if (keycloakId == null || keycloakId.trim().isEmpty()) {
                throw new RuntimeException("Keycloak ID not found in JWT token");
            }

            // Check if user already exists
            UserEntity existingUser = userRepository.findByKeycloakId(keycloakId).orElse(null);

            if (existingUser != null) {
                System.out.println("User already exists in database: " + keycloakId);

                // Ensure new fields have default values for existing users
                boolean updated = false;
                if (existingUser.getBalance() == null) {
                    existingUser.setBalance(BigDecimal.ZERO);
                    updated = true;
                }
                if (existingUser.getWithdrawableBalance() == null) {
                    existingUser.setWithdrawableBalance(BigDecimal.ZERO);
                    updated = true;
                }
                if (existingUser.getPendingWithdrawals() == null) {
                    existingUser.setPendingWithdrawals(BigDecimal.ZERO);
                    updated = true;
                }
                if (existingUser.getPendingDeposits() == null) {
                    existingUser.setPendingDeposits(BigDecimal.ZERO);
                    updated = true;
                }
                if (existingUser.getBonusBalance() == null) {
                    existingUser.setBonusBalance(BigDecimal.ZERO);
                    updated = true;
                }

                if (updated) {
                    return userRepository.save(existingUser);
                }

                return existingUser;
            }

            // User doesn't exist - fetch complete profile from Keycloak Admin API
            System.out.println("Creating new user from Keycloak: " + keycloakId);

            // Get basic info from JWT
            String username = getClaimSafely(jwt, "preferred_username");
            String email = getClaimSafely(jwt, "email");
            String firstName = getClaimSafely(jwt, "given_name");
            String lastName = getClaimSafely(jwt, "family_name");

            // Fetch complete user profile from Keycloak
            String phone = null;
            String country = null;
            String address = null;
            String postalNumber = null;
            LocalDate birthDate = null;
            String referralCode = null;

            try {
                UserRepresentation keycloakUser = keycloakService.getUserFromKeycloak(keycloakId);
                if (keycloakUser != null && keycloakUser.getAttributes() != null) {
                    Map<String, List<String>> attributes = keycloakUser.getAttributes();

                    phone = keycloakService.getAttributeValue(attributes, "phone");
                    country = keycloakService.getAttributeValue(attributes, "country");
                    address = keycloakService.getAttributeValue(attributes, "address");
                    postalNumber = keycloakService.getAttributeValue(attributes, "postalNumber");
                    referralCode = keycloakService.getAttributeValue(attributes, "referralCode");

                    String birthDateStr = keycloakService.getAttributeValue(attributes, "birthDate");
                    if (birthDateStr != null && !birthDateStr.trim().isEmpty()) {
                        try {
                            birthDate = LocalDate.parse(birthDateStr);
                        } catch (Exception e) {
                            System.err.println("Failed to parse birth date: " + birthDateStr);
                        }
                    }

                    System.out.println("Retrieved custom attributes from Keycloak:");
                    System.out.println("Phone: " + phone);
                    System.out.println("Country: " + country);
                    System.out.println("Address: " + address);
                    System.out.println("Postal: " + postalNumber);
                    System.out.println("Birth Date: " + birthDate);
                }
            } catch (Exception e) {
                System.err.println("Warning: Could not fetch custom attributes from Keycloak: " + e.getMessage());
            }

            // Create user with all available information
            UserEntity newUser = createOrUpdateUser(
                    keycloakId,
                    username != null ? username : "user_" + keycloakId.substring(0, 8),
                    email != null ? email : "",
                    firstName != null ? firstName : "",
                    lastName != null ? lastName : "",
                    phone,
                    country,
                    address,
                    postalNumber,
                    birthDate,
                    true,               // termsAccepted
                    true,               // active
                    BigDecimal.ZERO,    // balance
                    BigDecimal.ZERO,    // withdrawableBalance
                    BigDecimal.ZERO,    // pendingWithdrawals
                    BigDecimal.ZERO,    // pendingDeposits
                    null                // bannedUntil
            );

            // Set referral code if available
            if (referralCode != null) {
                newUser.setReferralCode(referralCode);
                userRepository.save(newUser);
            }

            System.out.println("Successfully created user from Keycloak with all custom fields: " + keycloakId);
            return newUser;

        } catch (Exception e) {
            System.err.println("Failed to ensure user from token: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to synchronize user with database: " + e.getMessage(), e);
        }
    }

    @Transactional
    public UserEntity updateCurrentUserProfile(UserProfileUpdateRequest request) {
        Long userId = getCurrentAppUserId();
        return updateUserProfile(userId, request);
    }

    private String getClaimSafely(Jwt jwt, String claimName) {
        try {
            Object claim = jwt.getClaims().get(claimName);
            return claim != null ? claim.toString().trim() : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional
    public UserEntity acceptCurrentUserTerms() {
        Long userId = getCurrentAppUserId();
        return acceptTerms(userId);
    }

    public UserStatsResponse getCurrentUserStats() {
        Long userId = getCurrentAppUserId();
        return getUserStats(userId);
    }

    public UserEntity getCurrentUser() {
        Long userId = getCurrentAppUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ======== User Operations (internal / reused) ========
    public UserEntity createOrUpdateUser(
            String keycloakId, String username, String email, String firstName, String lastName,
            String phone, String country, String address, String postalNumber, LocalDate birthDate,
            boolean termsAccepted, boolean active, BigDecimal balance,
            BigDecimal withdrawableBalance,
            BigDecimal pendingWithdrawals,          // NEW
            BigDecimal pendingDeposits,             // NEW
            LocalDateTime bannedUntil
    )
 {
        UserEntity user = userRepository.findByKeycloakId(keycloakId).orElse(new UserEntity());
        user.setKeycloakId(keycloakId);
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhone(phone);
        user.setCountry(country);
        user.setAddress(address);
        user.setPostalNumber(postalNumber);
        user.setBirthDate(birthDate);
        user.setTermsAccepted(termsAccepted);
        user.setActive(active);
        user.setBalance(balance);
        user.setWithdrawableBalance(withdrawableBalance);
        user.setPendingWithdrawals(pendingWithdrawals);
        user.setPendingDeposits(pendingDeposits);
        user.setBannedUntil(bannedUntil);

        return userRepository.save(user);
    }

    @Transactional
    public UserEntity updateUserProfile(Long userId, UserProfileUpdateRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getCountry() != null) user.setCountry(request.getCountry());
        if (request.getAddress() != null) user.setAddress(request.getAddress());
        if (request.getPostalNumber() != null) user.setPostalNumber(request.getPostalNumber());
        if (request.getBirthDate() != null) user.setBirthDate(request.getBirthDate());

        return userRepository.save(user);
    }

    @Transactional
    public UserEntity acceptTerms(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setTermsAccepted(true);
        return userRepository.save(user);
    }

    private UserStatsResponse getUserStats(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserSessionStats sessionStats = sessionParticipationService.getUserSessionStats(userId);

        return new UserStatsResponse(
                user.getId(),
                user.getUsername(),
                sessionStats.getTotalSessions(),
                sessionStats.getWonSessions(),
                sessionStats.getWinRate(),
                sessionStats.getTotalWinnings(),
                sessionStats.getTotalSpent(),
                sessionStats.getNetProfit(),
                sessionStats.getAverageAccuracy()
        );
    }

    private String generateReferralCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (userRepository.existsByReferralCode(code));
        return code;
    }

    // ======== Admin / Global Operations ========
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    public List<UserEntity> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public void deleteUser(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        userRepository.delete(user);
    }

    @Transactional
    public void creditBalance(Long userId, BigDecimal amount, Long adminId) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        BigDecimal oldBalance = user.getBalance();
        BigDecimal oldWithdrawable = user.getWithdrawableBalance();

        // Add to both balance and withdrawable balance for admin credits
        user.setBalance(user.getBalance().add(amount));
        user.setWithdrawableBalance(user.getWithdrawableBalance().add(amount));
        userRepository.save(user);

        // Update audit details
        UserManagementAudit audit = new UserManagementAudit();
        audit.setUserId(userId);
        audit.setAdminId(adminId);
        audit.setAction(UserAction.CREDIT);
        audit.setDetails("Credited " + amount + " | Previous balance: " + oldBalance +
                " | New balance: " + user.getBalance() +
                " | Previous withdrawable: " + oldWithdrawable +
                " | New withdrawable: " + user.getWithdrawableBalance());
        audit.setTimestamp(LocalDateTime.now());
        userManagementAuditRepository.save(audit);
    }

    @Transactional
    public void debitBalance(Long userId, BigDecimal amount, Long adminId) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check against withdrawable balance for debits
        if (user.getWithdrawableBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    String.valueOf(userId),
                    amount.toString(),
                    user.getWithdrawableBalance().toString()
            );
        }

        BigDecimal oldBalance = user.getBalance();
        BigDecimal oldWithdrawable = user.getWithdrawableBalance();

        // Deduct from both balance and withdrawable balance
        user.setBalance(user.getBalance().subtract(amount));
        user.setWithdrawableBalance(user.getWithdrawableBalance().subtract(amount));
        userRepository.save(user);

        // Update audit details
        UserManagementAudit audit = new UserManagementAudit();
        audit.setUserId(userId);
        audit.setAdminId(adminId);
        audit.setAction(UserAction.DEBIT);
        audit.setDetails("Debited " + amount + " | Previous balance: " + oldBalance +
                " | New balance: " + user.getBalance() +
                " | Previous withdrawable: " + oldWithdrawable +
                " | New withdrawable: " + user.getWithdrawableBalance());
        audit.setTimestamp(LocalDateTime.now());
        userManagementAuditRepository.save(audit);
    }


    // ======== Inner DTO classes ========
    public static class UserProfileUpdateRequest {
        private String username;
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
        private String country;
        private String address;
        private String postalNumber;
        private LocalDate birthDate;

        public UserProfileUpdateRequest() {}

        public String getUsername() {return username;}
        public void setUsername(String username) {this.username = username;}

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getEmail() {return email;}
        public void setEmail(String email) { this.email = email; }
        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getPostalNumber() { return postalNumber; }
        public void setPostalNumber(String postalNumber) { this.postalNumber = postalNumber; }
        public LocalDate getBirthDate() { return birthDate; }
        public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    }

    public static class UserStatsResponse {
        private final Long userId;
        private final String username;
        private final int totalSessions;
        private final int wonSessions;
        private final double winRate;
        private final BigDecimal totalWinnings;
        private final BigDecimal totalSpent;
        private final BigDecimal netProfit;
        private final double averageAccuracy;

        public UserStatsResponse(Long userId, String username, int totalSessions,
                                 int wonSessions, double winRate, BigDecimal totalWinnings,
                                 BigDecimal totalSpent, BigDecimal netProfit, double averageAccuracy) {
            this.userId = userId;
            this.username = username;
            this.totalSessions = totalSessions;
            this.wonSessions = wonSessions;
            this.winRate = winRate;
            this.totalWinnings = totalWinnings;
            this.totalSpent = totalSpent;
            this.netProfit = netProfit;
            this.averageAccuracy = averageAccuracy;
        }

        public Long getUserId() { return userId; }
        public String getUsername() { return username; }
        public int getTotalSessions() { return totalSessions; }
        public int getWonSessions() { return wonSessions; }
        public double getWinRate() { return winRate; }
        public BigDecimal getTotalWinnings() { return totalWinnings; }
        public BigDecimal getTotalSpent() { return totalSpent; }
        public BigDecimal getNetProfit() { return netProfit; }
        public double getAverageAccuracy() { return averageAccuracy; }
    }
    @Transactional
    public void refundUser(Long userId, BigDecimal amount) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setBalance(user.getBalance().add(amount));
        user.setWithdrawableBalance(user.getWithdrawableBalance().add(amount));
        userRepository.save(user);
    }

    @Transactional
    public void banUserTemporarily(Long userId, int days, Long adminId, BanCause reason) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setBannedUntil(LocalDateTime.now().plusDays(days));
        userRepository.save(user);

        UserManagementAudit audit = new UserManagementAudit();
        audit.setUserId(userId);
        audit.setAdminId(adminId);
        audit.setAction(UserAction.TEMP_BAN);
        audit.setDetails("Banned for " + days + " day(s)");
        audit.setReason(reason); // ✅ Add the reason
        audit.setTimestamp(LocalDateTime.now());
        userManagementAuditRepository.save(audit);
    }

    @Transactional
    public void banUserPermanently(Long userId, Long adminId, BanCause reason) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setActive(false);
        userRepository.save(user);

        UserManagementAudit audit = new UserManagementAudit();
        audit.setUserId(userId);
        audit.setAdminId(adminId);
        audit.setAction(UserAction.PERMANENT_BAN);
        audit.setDetails("User permanently banned");
        audit.setReason(reason); // ✅ Add the reason
        audit.setTimestamp(LocalDateTime.now());
        userManagementAuditRepository.save(audit);
    }
    @Transactional
    public void unbanUser(Long userId, Long adminId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Check if the user is actually banned
        boolean isBanned = !user.isActive() ||
                (user.getBannedUntil() != null && user.getBannedUntil().isAfter(LocalDateTime.now()));

        if (!isBanned) {
            // User is not banned, nothing to do
            return;
        }

        // Unban the user
        user.setActive(true);
        user.setBannedUntil(null);
        userRepository.save(user);

        // Log audit
        UserManagementAudit audit = new UserManagementAudit();
        audit.setUserId(userId);
        audit.setAdminId(adminId);
        audit.setAction(UserAction.UNBAN);
        audit.setDetails("Ban removed");
        audit.setTimestamp(LocalDateTime.now());
        userManagementAuditRepository.save(audit);
    }


    public String getUserBanStatus(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!user.isActive()) return "permanently banned";
        if (user.getBannedUntil() != null && user.getBannedUntil().isAfter(LocalDateTime.now()))
            return "temporarily banned until " + user.getBannedUntil();
        return "active";
    }

public UserBalanceResponse getCurrentUserBalance() {
    UserEntity user = getCurrentUser();
    return new UserBalanceResponse(
            user.getBalance(),
            user.getWithdrawableBalance(),
            user.getPendingWithdrawals(),  // for withdrawals
            user.getPendingDeposits()      // for deposits
    );
}
public static class UserBalanceResponse {
    private final BigDecimal balance;
    private final BigDecimal withdrawableBalance;
    private final BigDecimal pendingWithdrawals;
    private final BigDecimal pendingDeposits;

    public UserBalanceResponse(BigDecimal balance, BigDecimal withdrawableBalance,
                               BigDecimal pendingWithdrawals, BigDecimal pendingDeposits) {
        this.balance = balance;
        this.withdrawableBalance = withdrawableBalance;
        this.pendingWithdrawals = pendingWithdrawals;
        this.pendingDeposits = pendingDeposits;
    }

    public BigDecimal getBalance() { return balance; }
    public BigDecimal getWithdrawableBalance() { return withdrawableBalance; }
    public BigDecimal getPendingWithdrawals() { return pendingWithdrawals; }
    public BigDecimal getPendingDeposits() { return pendingDeposits; }
}
}
