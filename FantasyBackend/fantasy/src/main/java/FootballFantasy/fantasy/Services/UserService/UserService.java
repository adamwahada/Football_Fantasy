package FootballFantasy.fantasy.Services.UserService;

import FootballFantasy.fantasy.Entities.UserEntity.UserEntity;
import FootballFantasy.fantasy.Exception.InsufficientBalanceException;
import FootballFantasy.fantasy.Repositories.UserRepository.UserRepository;
import FootballFantasy.fantasy.Services.GameweekService.SessionParticipationService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import FootballFantasy.fantasy.Dto.UserSessionStats;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SessionParticipationService sessionParticipationService;

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

    @Transactional
    public UserEntity ensureCurrentUserFromToken() {
        Jwt jwt = getJwt();

        String keycloakId = jwt.getSubject();
        String username    = (String) jwt.getClaims().getOrDefault("preferred_username", "");
        String email       = (String) jwt.getClaims().getOrDefault("email", "");
        String firstName   = (String) jwt.getClaims().getOrDefault("given_name", "");
        String lastName    = (String) jwt.getClaims().getOrDefault("family_name", "");

        // These are app-specific and may not be in the token
        String phone = null, country = null, address = null, postalNumber = null;
        LocalDate birthDate = null;

        return createOrUpdateUser(
                keycloakId, username, email, firstName, lastName,
                phone, country, address, postalNumber, birthDate
        );
    }

    @Transactional
    public UserEntity updateCurrentUserProfile(UserProfileUpdateRequest request) {
        Long userId = getCurrentAppUserId();
        return updateUserProfile(userId, request);
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

    @Transactional
    public UserEntity createOrUpdateUser(String keycloakId, String username, String email,
                                            String firstName, String lastName,
                                            String phone, String country, String address,
                                            String postalNumber, LocalDate birthDate) {

        Optional<UserEntity> existingUser = userRepository.findByKeycloakId(keycloakId);

        if (existingUser.isPresent()) {
            UserEntity user = existingUser.get();
            user.setUsername(username);
            user.setEmail(email);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setPhone(phone);
            user.setCountry(country);
            user.setAddress(address);
            user.setPostalNumber(postalNumber);
            user.setBirthDate(birthDate);
            return userRepository.save(user);
        } else {
            UserEntity newUser = new UserEntity();
            newUser.setKeycloakId(keycloakId);
            newUser.setUsername(username);
            newUser.setEmail(email);
            newUser.setFirstName(firstName);
            newUser.setLastName(lastName);
            newUser.setPhone(phone);
            newUser.setCountry(country);
            newUser.setAddress(address);
            newUser.setPostalNumber(postalNumber);
            newUser.setBirthDate(birthDate);
            newUser.setTermsAccepted(true);
            newUser.setReferralCode(generateReferralCode());
            return userRepository.save(newUser);
        }
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
    public void refundUser(Long userId, BigDecimal amount) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setBalance(user.getBalance().add(amount));
        userRepository.save(user);
    }

    @Transactional
    public void creditBalance(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setBalance(user.getBalance().add(amount));
        userRepository.save(user);

        System.out.println("💰 Admin credited " + amount + " to user " + userId + "; new balance: " + user.getBalance());
    }
    @Transactional
    public void debitBalance(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    String.valueOf(userId),
                    amount.toString(),
                    user.getBalance().toString()
            );
        }

        user.setBalance(user.getBalance().subtract(amount));
        userRepository.save(user);

        System.out.println("💸 Admin debited " + amount + " from user " + userId + "; new balance: " + user.getBalance());
    }

    // ======== Inner DTO classes ========
    public static class UserProfileUpdateRequest {
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
        private String country;
        private String address;
        private String postalNumber;
        private LocalDate birthDate;

        public UserProfileUpdateRequest() {}

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
    public void banUserTemporarily(Long userId, int days) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setBannedUntil(LocalDate.now().plusDays(days));
        userRepository.save(user);
    }

    @Transactional
    public void banUserPermanently(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setActive(false);
        userRepository.save(user);
    }

    @Transactional
    public void unbanUser(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setActive(true);
        user.setBannedUntil(null);
        userRepository.save(user);
    }
    public String getUserBanStatus(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        if (!user.isActive()) return "permanently banned";
        if (user.getBannedUntil() != null && user.getBannedUntil().isAfter(LocalDate.now()))
            return "temporarily banned until " + user.getBannedUntil();
        return "active";
    }

}
