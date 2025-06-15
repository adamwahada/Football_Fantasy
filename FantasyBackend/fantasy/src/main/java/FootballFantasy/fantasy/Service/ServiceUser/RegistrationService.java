package FootballFantasy.fantasy.Service.ServiceUser;

import FootballFantasy.fantasy.Dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final RecaptchaService recaptchaService;
    private final KeycloakService keycloakService;

    public void registerUser(RegisterRequest request) {
        // 1. Verify reCAPTCHA token
        boolean captchaVerified = recaptchaService.verifyToken(request.getRecaptchaToken());
        if (!captchaVerified) {
            throw new IllegalArgumentException("reCAPTCHA verification failed");
        }

        // 2. Check terms accepted
        if (!request.isTermsAccepted()) {
            throw new IllegalArgumentException("You must accept the terms and conditions");
        }


        // 4. Create user in Keycloak
        keycloakService.createUser(request);
    }
}
