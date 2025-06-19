package FootballFantasy.fantasy.Service.ServiceUser;

import FootballFantasy.fantasy.Dto.RegisterRequest;
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
        // First, print configuration to verify environment variables
        printConfiguration();

        Keycloak keycloak = null;
        try {
            log.info("🔍 DEBUG - Configuration:");
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

            // 2. Test connection first
            log.info("🔍 Testing connection...");
            try {
                var realmRepresentation = keycloak.realm(realm).toRepresentation();
                log.info("✅ Successfully connected to realm: {}", realmRepresentation.getRealm());
            } catch (Exception e) {
                log.error("❌ Failed to connect to realm: {}", e.getMessage());
                throw new RuntimeException("Failed to connect to Keycloak realm", e);
            }

            // 3. Test if we can access users resource
            log.info("🔍 Testing users resource access...");
            try {
                UsersResource usersResource = keycloak.realm(realm).users();
                int userCount = usersResource.count();
                log.info("✅ Can access users resource. Current user count: {}", userCount);
            } catch (Exception e) {
                log.error("❌ Cannot access users resource: {}", e.getMessage());
                throw new RuntimeException("Cannot access users resource - check service account permissions", e);
            }

            // 4. Create user representation
            UserRepresentation user = buildUserRepresentation(request);
            log.info("🔍 User representation created for: {}", user.getUsername());

            // 5. Create user
            UsersResource usersResource = keycloak.realm(realm).users();
            log.info("🔄 Creating user in Keycloak...");

            Response response = usersResource.create(user);
            log.info("🔍 Response status: {}", response.getStatus());

            if (response.getStatus() == 201) {
                log.info("✅ User created successfully");

                String userId = extractUserIdFromResponse(response);
                setUserPassword(usersResource, userId, request.getPassword());
                assignUserRole(keycloak, usersResource, userId);

                log.info("✅ User setup completed: {}", userId);
            } else if (response.getStatus() == 409) {
                log.error("❌ User already exists: {}", request.getUsername());
                throw new RuntimeException("User already exists: " + request.getUsername());
            } else {
                String errorDetails = "No details available";
                try {
                    errorDetails = response.readEntity(String.class);
                } catch (Exception e) {
                    log.warn("Could not read error response: {}", e.getMessage());
                }

                log.error("❌ Failed to create user. Status: {}, Details: {}",
                        response.getStatus(), errorDetails);

                // Specific handling for 403
                if (response.getStatus() == 403) {
                    throw new RuntimeException("Permission denied. Check if service account has 'manage-users' role assigned. Error details: " + errorDetails);
                }

                throw new RuntimeException("Failed to create user (Status: " + response.getStatus() + "): " + errorDetails);
            }

        } catch (Exception e) {
            log.error("❌ Error in createUser: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create user: " + e.getMessage(), e);
        } finally {
            if (keycloak != null) {
                try {
                    keycloak.close();
                } catch (Exception e) {
                    log.warn("⚠️ Error closing Keycloak client: {}", e.getMessage());
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