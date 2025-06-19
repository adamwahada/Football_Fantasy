package FootballFantasy.fantasy.Service.ServiceUser;

import FootballFantasy.fantasy.Dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationService {

    private final RecaptchaService recaptchaService;
    private final KeycloakService keycloakService;

    public void registerUser(RegisterRequest request) {
        log.info("🔄 Début du processus d'enregistrement pour: {}", request.getUsername());

        // 1. Vérifier le token reCAPTCHA
        log.debug("🔍 Vérification du token reCAPTCHA...");
        boolean captchaVerified = recaptchaService.verifyToken(request.getRecaptchaToken());
        if (!captchaVerified) {
            log.warn("❌ Échec de la vérification reCAPTCHA pour: {}", request.getUsername());
            throw new IllegalArgumentException("Échec de la vérification reCAPTCHA");
        }
        log.debug("✅ Token reCAPTCHA vérifié avec succès");

        // 2. Vérifier l'acceptation des conditions
        if (!request.isTermsAccepted()) {
            log.warn("❌ Conditions non acceptées pour: {}", request.getUsername());
            throw new IllegalArgumentException("Vous devez accepter les conditions générales");
        }
        log.debug("✅ Conditions générales acceptées");

        // 3. Créer l'utilisateur dans Keycloak
        try {
            log.debug("🔄 Création de l'utilisateur dans Keycloak...");
            keycloakService.createUser(request);
            log.info("✅ Utilisateur créé avec succès dans Keycloak: {}", request.getUsername());
        } catch (Exception e) {
            log.error("❌ Erreur lors de la création de l'utilisateur dans Keycloak: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur lors de la création du compte utilisateur", e);
        }
    }
}