package FootballFantasy.fantasy.Services.UserService;

import FootballFantasy.fantasy.Dto.RegisterRequest;
import FootballFantasy.fantasy.Exception.UserAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationService {

    private final RecaptchaService recaptchaService;
    private final KeycloakService keycloakService;
    private final ReferralCodeService referralCodeService;

    public void registerUser(RegisterRequest request) {
        try {
            // ✅ STEP 1: Check referral code only if provided
            if (request.getReferralCode() != null && !request.getReferralCode().isBlank()) {
                if (!referralCodeService.isCodeValid(request.getReferralCode())) {
                    log.warn("Code de parrainage invalide ou expiré, mais on continue la création du compte : {}", request.getReferralCode());
                    // On annule le code pour ne pas l’envoyer à Keycloak
                    request.setReferralCode(null);
                }
            }

            // ✅ STEP 2: Continue to Keycloak
            keycloakService.createUser(request);

            // ✅ STEP 3: Mark referral code as used after success
            if (request.getReferralCode() != null && !request.getReferralCode().isBlank()) {
                referralCodeService.markCodeAsUsed(request.getReferralCode());
            }

        } catch (UserAlreadyExistsException e) {
            log.warn("❗ UserEntity already exists in RegistrationService: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("❌ Erreur lors de la création de l'utilisateur dans Keycloak: {}", e.getMessage());
            throw new RuntimeException("Erreur lors de la création du compte utilisateur", e);
        }
    }
}