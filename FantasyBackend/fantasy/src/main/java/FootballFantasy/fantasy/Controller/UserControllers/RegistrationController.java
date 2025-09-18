package FootballFantasy.fantasy.Controller.UserControllers;

import FootballFantasy.fantasy.Dto.RegisterRequest;
import FootballFantasy.fantasy.Exception.UserAlreadyExistsException;
import FootballFantasy.fantasy.Services.UserService.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
public class RegistrationController {

    private final RegistrationService registrationService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request, BindingResult bindingResult) {

        log.info("🚀 Registration request received for username: {}", request.getUsername());
        log.debug("📝 Registration data: email={}, firstName={}, lastName={}",
                request.getEmail(), request.getFirstName(), request.getLastName());

        // Handle validation errors
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            for (FieldError error : bindingResult.getFieldErrors()) {
                errors.put(error.getField(), error.getDefaultMessage());
                log.warn("❌ Validation error - {}: {}", error.getField(), error.getDefaultMessage());
            }
            log.warn("❌ Validation errors during registration: {}", errors);
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Données d'inscription invalides. Veuillez vérifier vos informations.",
                    "errors", errors
            ));
        }

        // Additional custom validation
        if (!request.isValidAge()) {
            log.warn("❌ Invalid age for user: {}", request.getUsername());
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Âge invalide. Vous devez avoir entre 13 et 120 ans."
            ));
        }

        if (request.getTermsAccepted() == null || !request.getTermsAccepted()) {
            log.warn("❌ Terms not accepted for user: {}", request.getUsername());
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Vous devez accepter les conditions d'utilisation."
            ));
        }

        try {
            log.info("🚀 Starting user registration process for: {}", request.getUsername());
            registrationService.registerUser(request);

            log.info("✅ User registration completed successfully for: {}", request.getUsername());
            return ResponseEntity.ok(Map.of(
                    "message", "Inscription réussie ! Un email de vérification a été envoyé à votre adresse.",
                    "username", request.getUsername(),
                    "email", request.getEmail()
            ));

        } catch (UserAlreadyExistsException e) {
            log.warn("❗ User already exists during registration: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "message", e.getMessage()
            ));

        } catch (RuntimeException e) {
            log.error("❌ Registration failed for user {}: {}", request.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "message", e.getMessage()
            ));

        } catch (Exception e) {
            log.error("❌ Unexpected error during registration for user {}: {}",
                    request.getUsername(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "message", "Erreur interne du serveur. Veuillez réessayer plus tard."
            ));
        }
    }

    @GetMapping("/check-availability")
    public ResponseEntity<?> checkAvailability(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email) {

        Map<String, Boolean> availability = new HashMap<>();

        if (username != null && !username.trim().isEmpty()) {
            // You'll need to inject UserService here or create a method in RegistrationService
            // For now, assuming you have access to UserService
            // availability.put("usernameAvailable", userService.isUsernameAvailable(username));
        }

        if (email != null && !email.trim().isEmpty()) {
            // availability.put("emailAvailable", userService.isEmailAvailable(email));
        }

        return ResponseEntity.ok(availability);
    }
}