package FootballFantasy.fantasy.Service.ServiceUser;

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

    public void registerUser(RegisterRequest request) {
        try {
            keycloakService.createUser(request);
        } catch (UserAlreadyExistsException e) {
            log.warn("❗ User already exists in RegistrationService: {}", e.getMessage());
            throw e; // 🔥 Important : relancer telle quelle pour qu'elle soit gérée par le handler
        } catch (Exception e) {
            log.error("❌ Erreur lors de la création de l'utilisateur dans Keycloak: {}", e.getMessage());
            throw new RuntimeException("Erreur lors de la création du compte utilisateur", e);
        }
    }
}