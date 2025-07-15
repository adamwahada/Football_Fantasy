package FootballFantasy.fantasy.Services.UserService;

import FootballFantasy.fantasy.Entities.UserEntity.UserEntity;
import FootballFantasy.fantasy.Repositories.UserRepository.UserRepository;
import FootballFantasy.fantasy.Services.GameweekService.SessionParticipationService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
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


    /**
     * Create or update user from Keycloak token
     */
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
            newUser.setTermsAccepted(false);
            newUser.setReferralCode(generateReferralCode());
            return userRepository.save(newUser);
        }
    }

    /**
     * Get user by ID
     */
    public Optional<UserEntity> getUserById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Get user by Keycloak ID
     */
    public Optional<UserEntity> getUserByKeycloakId(String keycloakId) {
        return userRepository.findByKeycloakId(keycloakId);
    }

    /**
     * Get user by username
     */
    public Optional<UserEntity> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Get user by email
     */
    public Optional<UserEntity> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Update user profile
     */
    @Transactional
    public UserEntity updateUserProfile(Long userId, UserProfileUpdateRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getCountry() != null) {
            user.setCountry(request.getCountry());
        }
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }
        if (request.getPostalNumber() != null) {
            user.setPostalNumber(request.getPostalNumber());
        }
        if (request.getBirthDate() != null) {
            user.setBirthDate(request.getBirthDate());
        }

        return userRepository.save(user);
    }

    /**
     * Accept terms and conditions
     */
    @Transactional
    public UserEntity acceptTerms(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setTermsAccepted(true);
        return userRepository.save(user);
    }

    /**
     * Check if username is available
     */
    public boolean isUsernameAvailable(String username) {
        return !userRepository.existsByUsername(username);
    }

    /**
     * Check if email is available
     */
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    /**
     * Get all users (admin only)
     */
    public List<UserEntity> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Delete user account
     */
    @Transactional
    public void deleteUser(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Note: This will cascade delete all session participations
        userRepository.delete(user);
    }

    /**
     * Get user statistics
     */
    public UserStatsResponse getUserStats(Long userId) {
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

    /**
     * Generate unique referral code
     */
    private String generateReferralCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (userRepository.existsByReferralCode(code));
        return code;
    }

    // ===== INNER CLASSES =====

    /**
     * User profile update request
     */
    public static class UserProfileUpdateRequest {
        private String firstName;
        private String lastName;
        private String phone;
        private String country;
        private String address;
        private String postalNumber;
        private LocalDate birthDate;

        // Constructors
        public UserProfileUpdateRequest() {}

        // Getters and setters
        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }

        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }

        public String getPostalNumber() { return postalNumber; }
        public void setPostalNumber(String postalNumber) { this.postalNumber = postalNumber; }

        public LocalDate getBirthDate() { return birthDate; }
        public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    }

    /**
     * User statistics response
     */
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

        // Getters
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
}