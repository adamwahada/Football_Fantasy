package FootballFantasy.fantasy.Services.UserService;

import FootballFantasy.fantasy.Dto.RegisterRequest;
import FootballFantasy.fantasy.Exception.UserAlreadyExistsException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class KeycloakService {

    @Value("${keycloak.backend.server-url}")
    private String serverUrl;

    @Value("${keycloak.backend.realm}")
    private String realm;

    @Value("${keycloak.backend.client-id}")
    private String clientId;

    @Value("${keycloak.backend.client-secret}")
    private String clientSecret;

    // =======================
    // USER CREATION METHODS
    // =======================
    public String createUser(RegisterRequest request) {
        printConfiguration();

        Keycloak keycloak = null;
        try {
            keycloak = KeycloakBuilder.builder()
                    .serverUrl(serverUrl)
                    .realm(realm)
                    .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .build();

            UsersResource usersResource = keycloak.realm(realm).users();
            UserRepresentation user = buildUserRepresentation(request);

            log.info("📝 Creating user in Keycloak: {}", request.getUsername());
            Response response = usersResource.create(user);
            int status = response.getStatus();

            if (status == 201) {
                String userId = extractUserIdFromResponse(response);
                log.info("✅ User created in Keycloak with ID: {}", userId);

                // Set password
                setUserPassword(usersResource, userId, request.getPassword());
                log.info("✅ Password set successfully");

                // 🔥 KEY: Send email verification
                sendEmailVerification(usersResource, userId, request.getEmail());

                log.info("🎉 User setup completed successfully: {}", userId);
                return userId;

            } else if (status == 409) {
                String errorDetails = getErrorDetails(response);
                log.warn("❌ User already exists: {}", errorDetails);
                throw new UserAlreadyExistsException("Un compte avec ce nom d'utilisateur ou email existe déjà.");

            } else {
                String errorDetails = getErrorDetails(response);
                log.error("❌ Failed to create user. Status: {}, Details: {}", status, errorDetails);
                throw new RuntimeException("Échec de la création de l'utilisateur. Statut: " + status + ". Détails: " + errorDetails);
            }

        } catch (UserAlreadyExistsException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ Exception during user creation: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur inattendue lors de la création du compte utilisateur.", e);
        } finally {
            if (keycloak != null) keycloak.close();
        }
    }

    private void printConfiguration() {
        log.info("🔍 KEYCLOAK CONFIGURATION CHECK:");
        log.info("Server URL: {}", serverUrl);
        log.info("Realm: {}", realm);
        log.info("Client ID: {}", clientId);
        log.info("Client Secret: {}", clientSecret != null ? "***PROVIDED***" : "NULL");
    }

    private UserRepresentation buildUserRepresentation(RegisterRequest request) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEnabled(true); // ✅ Account is immediately active

        // ✅ Email is unverified but user can still login
        user.setEmailVerified(false);

        // Add custom attributes
        Map<String, List<String>> attributes = new HashMap<>();
        addAttributeIfPresent(attributes, "phone", request.getPhone());
        addAttributeIfPresent(attributes, "country", request.getCountry());
        addAttributeIfPresent(attributes, "address", request.getAddress());
        addAttributeIfPresent(attributes, "postalNumber", request.getPostalNumber());
        addAttributeIfPresent(attributes, "birthDate", request.getBirthDate());
        addAttributeIfPresent(attributes, "referralCode", request.getReferralCode());
        attributes.put("termsAccepted", List.of(String.valueOf(request.isTermsAccepted())));
        user.setAttributes(attributes);

        // 🔥 REMOVE THIS LINE - no required actions needed
        // user.setRequiredActions(Arrays.asList("VERIFY_EMAIL"));

        return user;
    }
    private void addAttributeIfPresent(Map<String, List<String>> attributes, String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            attributes.put(key, List.of(value.trim()));
        }
    }

    private String extractUserIdFromResponse(Response response) {
        String path = response.getLocation().getPath();
        return path.replaceAll(".*/([^/]+)$", "$1");
    }

    private void setUserPassword(UsersResource usersResource, String userId, String password) {
        try {
            CredentialRepresentation cred = new CredentialRepresentation();
            cred.setType(CredentialRepresentation.PASSWORD);
            cred.setTemporary(false);
            cred.setValue(password);
            usersResource.get(userId).resetPassword(cred);
            log.info("✅ Password set successfully for user: {}", userId);
        } catch (Exception e) {
            log.error("❌ Failed to set password: {}", e.getMessage());
            throw new RuntimeException("Erreur lors de la définition du mot de passe: " + e.getMessage());
        }
    }

    private void assignUserRole(Keycloak keycloak, UsersResource usersResource, String userId) {
        try {
            RoleRepresentation role = keycloak.realm(realm).roles().get("user").toRepresentation();
            usersResource.get(userId).roles().realmLevel().add(Collections.singletonList(role));
            log.info("✅ Role 'user' assigned successfully");
        } catch (Exception e) {
            log.error("❌ Failed to assign role: {}", e.getMessage());
            log.warn("⚠️ User created but role assignment failed. User may need role assigned manually.");
        }
    }


    private void sendEmailVerification(UsersResource usersResource, String userId, String email) {
        try {
            // ✅ Use executeActionsEmail for seamless verification
            List<String> actions = Arrays.asList("VERIFY_EMAIL");

            usersResource.get(userId).executeActionsEmail(
                    "angular-client",                           // Your Angular client ID
                    "http://localhost:4200/user-gameweek-list", // Direct redirect to main page
                    actions
            );

            log.info("✅ Seamless email verification sent to: {}", email);

        } catch (Exception e) {
            log.error("❌ Failed to send email verification: {}", e.getMessage(), e);
            // Don't throw - user account should still work even if email fails
            log.warn("⚠️ User can still login, but email verification failed");
        }
    }
    private String getErrorDetails(Response response) {
        try {
            return response.readEntity(String.class);
        } catch (Exception e) {
            return "Non spécifié";
        }
    }

    // =======================
    // JWT METHODS FOR LOGGED-IN USERS
    // =======================
    public Jwt getCurrentJwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt)) {
            throw new RuntimeException("User not authenticated");
        }
        return (Jwt) auth.getPrincipal();
    }

    public String getCurrentKeycloakId() {
        return getCurrentJwt().getSubject();
    }

    public String getCurrentUsername() {
        return (String) getCurrentJwt().getClaims().getOrDefault("preferred_username", "");
    }

    public String getCurrentEmail() {
        return (String) getCurrentJwt().getClaims().getOrDefault("email", "");
    }

    public String getCurrentFirstName() {
        return (String) getCurrentJwt().getClaims().getOrDefault("given_name", "");
    }

    public String getCurrentLastName() {
        return (String) getCurrentJwt().getClaims().getOrDefault("family_name", "");
    }

    public boolean isCurrentUserEmailVerified() {
        Jwt jwt = getCurrentJwt();
        Object emailVerified = jwt.getClaims().get("email_verified");
        return emailVerified != null && (Boolean) emailVerified;
    }
}