package FootballFantasy.fantasy.Services.UserService;

import FootballFantasy.fantasy.Dto.RegisterRequest;
import FootballFantasy.fantasy.Exception.UserAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationService {

    private final RecaptchaService recaptchaService;
    private final KeycloakService keycloakService;
    private final ReferralCodeService referralCodeService;
    private final UserService userService;

    @Transactional
    public void registerUser(RegisterRequest request) {
        try {
            log.info("🚀 Starting registration process for user: {}", request.getUsername());

            // ✅ STEP 1: Validate reCAPTCHA (uncomment if needed)
            // if (!recaptchaService.validateToken(request.getRecaptchaToken())) {
            //     throw new RuntimeException("Validation reCAPTCHA échouée");
            // }

            // ✅ STEP 2: Check if username and email are available
            if (!userService.isUsernameAvailable(request.getUsername())) {
                throw new UserAlreadyExistsException("Ce nom d'utilisateur est déjà utilisé");
            }

            if (!userService.isEmailAvailable(request.getEmail())) {
                throw new UserAlreadyExistsException("Cette adresse email est déjà utilisée");
            }

            // ✅ STEP 3: Validate and process birthdate
            LocalDate birthDate = null;
            if (request.getBirthDate() != null && !request.getBirthDate().trim().isEmpty()) {
                try {
                    birthDate = LocalDate.parse(request.getBirthDate());
                    log.info("✅ Birth date parsed successfully: {}", birthDate);
                } catch (DateTimeParseException e) {
                    log.error("❌ Invalid birth date format: {}", request.getBirthDate());
                    throw new RuntimeException("Format de date de naissance invalide. Utilisez le format YYYY-MM-DD");
                }
            }

            // ✅ STEP 4: Validate referral code if provided
            if (request.getReferralCode() != null && !request.getReferralCode().trim().isEmpty()) {
                if (!referralCodeService.isCodeValid(request.getReferralCode())) {
                    log.warn("❌ Code de parrainage invalide: {}", request.getReferralCode());
                    throw new RuntimeException("Code de parrainage invalide ou expiré");
                }
                log.info("✅ Referral code validated: {}", request.getReferralCode());
            }

            // ✅ STEP 5: Create user in Keycloak and CAPTURE the ID
            log.info("📝 Creating user in Keycloak...");
            String keycloakId = keycloakService.createUser(request);  // ✅ Capture the returned ID
            log.info("✅ User created in Keycloak successfully with ID: {}", keycloakId);

            // ✅ STEP 6: Create user in application database
            log.info("📝 Creating user in application database...");
            try {
                userService.createOrUpdateUser(
                        keycloakId,  // ✅ Pass the actual Keycloak ID instead of null
                        request.getUsername(),
                        request.getEmail(),
                        request.getFirstName(),
                        request.getLastName(),
                        request.getPhone(),
                        request.getCountry(),
                        request.getAddress(),
                        request.getPostalNumber(),
                        birthDate // Now properly parsed LocalDate
                );
                log.info("✅ User created in application database successfully with Keycloak ID: {}", keycloakId);
            } catch (Exception e) {
                log.error("❌ Failed to create user in application database: {}", e.getMessage(), e);
                // Since Keycloak user was created, we should clean it up or let it be handled on first login
                throw new RuntimeException("Erreur lors de la création du profil utilisateur: " + e.getMessage());
            }

            // ✅ STEP 7: Mark referral code as used
            if (request.getReferralCode() != null && !request.getReferralCode().trim().isEmpty()) {
                try {
                    referralCodeService.markCodeAsUsed(request.getReferralCode());
                    log.info("✅ Referral code marked as used: {}", request.getReferralCode());
                } catch (Exception e) {
                    log.warn("⚠️ Could not mark referral code as used: {}", e.getMessage());
                    // Don't fail registration for this
                }
            }


            log.info("🎉 Registration completed successfully for user: {}", request.getUsername());

        } catch (UserAlreadyExistsException e) {
            log.warn("❗ User already exists: {}", e.getMessage());
            throw e; // Re-throw as is
        } catch (RuntimeException e) {
            log.error("❌ Runtime error during registration: {}", e.getMessage());
            throw e; // Re-throw as is
        } catch (Exception e) {
            log.error("❌ Unexpected error during user registration: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur inattendue lors de l'inscription: " + e.getMessage());
        }
    }
}