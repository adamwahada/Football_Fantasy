package FootballFantasy.fantasy.Service.ServiceUser;

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
import org.springframework.stereotype.Service;

import java.util.*;

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

    // Debug method to check if environment variables are loaded correctly
    public void printConfiguration() {
        log.info("🔍 CONFIGURATION CHECK:");
        log.info("Server URL: {}", serverUrl);
        log.info("Realm: {}", realm);
        log.info("Client ID: {}", clientId);
        log.info("Client Secret: {}", clientSecret != null ? clientSecret.substring(0, 8) + "..." : "NULL");
        log.info("Expected: http://localhost:8180, football-fantasy, fantasy-backend");
    }

    public void createUser(RegisterRequest request) {
        printConfiguration(); // Optional debug

        Keycloak keycloak = null;

        try {
            log.info("🔍 Configuration:");
            log.info("Server URL: {}", serverUrl);
            log.info("Realm: {}", realm);
            log.info("Client ID: {}", clientId);
            log.info("Client Secret: {}", clientSecret != null ? "***PROVIDED***" : "NULL");

            // 1. Connect as service account
            log.info("🔄 Building Keycloak client...");
            keycloak = KeycloakBuilder.builder()
                    .serverUrl(serverUrl)
                    .realm(realm)
                    .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .build();

            // 2. Test connection
            log.info("🔍 Testing connection...");
            try {
                var realmRepresentation = keycloak.realm(realm).toRepresentation();
                log.info("✅ Connected to realm: {}", realmRepresentation.getRealm());
            } catch (Exception e) {
                log.error("❌ Connection to realm failed: {}", e.getMessage());
                throw new RuntimeException("Impossible de se connecter au serveur d'authentification.", e);
            }

            // 3. Test user access
            log.info("🔍 Checking access to users resource...");
            UsersResource usersResource = keycloak.realm(realm).users();
            try {
                int userCount = usersResource.count();
                log.info("✅ Access to users resource confirmed. Current count: {}", userCount);
            } catch (Exception e) {
                log.error("❌ Access to users resource failed: {}", e.getMessage());
                throw new RuntimeException("Accès refusé à la ressource utilisateurs. Vérifiez les permissions du client.", e);
            }

            // 4. Create user representation
            UserRepresentation user = buildUserRepresentation(request);
            log.info("🔧 Prepared user representation for: {}", user.getUsername());

            // 5. Create user
            log.info("🚀 Creating user...");
            Response response = usersResource.create(user);
            int status = response.getStatus();
            log.info("📩 Response status: {}", status);

            if (status == 201) {
                log.info("✅ User created successfully.");

                String userId = extractUserIdFromResponse(response);
                setUserPassword(usersResource, userId, request.getPassword());
                assignUserRole(keycloak, usersResource, userId);

                log.info("🎉 User setup completed successfully: {}", userId);

            } else if (status == 409) {
                log.warn("⚠️ User already exists: {}", request.getUsername());
                throw new UserAlreadyExistsException("Un compte avec ce nom d'utilisateur ou email existe déjà.");
            } else {
                String errorDetails = "Non spécifié";
                try {
                    errorDetails = response.readEntity(String.class);
                } catch (Exception e) {
                    log.warn("⚠️ Impossible de lire le corps de la réponse: {}", e.getMessage());
                }

                log.error("❌ Erreur lors de la création de l'utilisateur. Statut: {}, Détails: {}", status, errorDetails);

                if (status == 403) {
                    throw new RuntimeException("Permission refusée : vérifiez que le client Keycloak possède le rôle 'manage-users'.");
                }

                throw new RuntimeException("Échec de la création de l'utilisateur. Statut: " + status + ". Détails: " + errorDetails);
            }

        } catch (UserAlreadyExistsException e) {
            throw e; // Let the controller advice handle it
        } catch (Exception e) {
            log.error("❌ Exception during user creation: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur inattendue lors de la création du compte utilisateur.", e);
        } finally {
            if (keycloak != null) {
                try {
                    keycloak.close();
                } catch (Exception e) {
                    log.warn("⚠️ Erreur lors de la fermeture du client Keycloak: {}", e.getMessage());
                }
            }
        }
    }

    private UserRepresentation buildUserRepresentation(RegisterRequest request) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEnabled(true);
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
        log.info("🔍 User attributes: {}", attributes);

        return user;
    }

    private void addAttributeIfPresent(Map<String, List<String>> attributes, String key, String value) {
        if (value != null && !value.trim().isEmpty()) {
            attributes.put(key, List.of(value.trim()));
        }
    }

    private String extractUserIdFromResponse(Response response) {
        String location = response.getLocation().getPath();
        String userId = location.replaceAll(".*/([^/]+)$", "$1");
        log.info("🔍 Extracted user ID: {}", userId);
        return userId;
    }

    private void setUserPassword(UsersResource usersResource, String userId, String password) {
        try {
            CredentialRepresentation passwordCred = new CredentialRepresentation();
            passwordCred.setTemporary(false);
            passwordCred.setType(CredentialRepresentation.PASSWORD);
            passwordCred.setValue(password);

            usersResource.get(userId).resetPassword(passwordCred);
            log.info("✅ Password set for user: {}", userId);
        } catch (Exception e) {
            log.error("❌ Failed to set password: {}", e.getMessage());
            throw new RuntimeException("Failed to set user password: " + e.getMessage(), e);
        }
    }

    private void assignUserRole(Keycloak keycloak, UsersResource usersResource, String userId) {
        try {
            RoleRepresentation userRole = keycloak.realm(realm).roles().get("user").toRepresentation();
            usersResource.get(userId).roles().realmLevel().add(Collections.singletonList(userRole));
            log.info("✅ User role assigned to: {}", userId);
        } catch (Exception e) {
            log.warn("⚠️ Failed to assign user role (user might still be functional): {}", e.getMessage());
            // Don't throw exception here as user creation was successful
        }
    }

    // Debug method - add this to a test controller
    public void debugKeycloakConnection() {
        Keycloak keycloak = null;
        try {
            log.info("🔍 DEBUG CONNECTION TEST");
            log.info("Server URL: {}", serverUrl);
            log.info("Realm: {}", realm);
            log.info("Client ID: {}", clientId);

            keycloak = KeycloakBuilder.builder()
                    .serverUrl(serverUrl)
                    .realm(realm)
                    .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .build();

            // Test realm access
            var realmRep = keycloak.realm(realm).toRepresentation();
            log.info("✅ Realm access OK: {}", realmRep.getRealm());

            // Test users resource
            var users = keycloak.realm(realm).users();
            var count = users.count();
            log.info("✅ Users resource access OK. Count: {}", count);

            // Test roles
            var roles = keycloak.realm(realm).roles().list();
            log.info("✅ Roles access OK. Found {} roles", roles.size());

        } catch (Exception e) {
            log.error("❌ Debug connection failed: {}", e.getMessage(), e);
        } finally {
            if (keycloak != null) {
                keycloak.close();
            }
        }
    }
}