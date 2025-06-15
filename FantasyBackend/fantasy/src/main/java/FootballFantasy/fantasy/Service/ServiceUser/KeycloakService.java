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

    @Value("${keycloak.backend.admin.username}")
    private String adminUsername;

    @Value("${keycloak.backend.admin.password}")
    private String adminPassword;

    public void createUser(RegisterRequest request) {
        // 1. Connect as admin
        Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm("master")
                .grantType(OAuth2Constants.PASSWORD)
                .clientId("admin-cli")
                .username(adminUsername)
                .password(adminPassword)
                .build();

        // 2. Create user
        UserRepresentation user = new UserRepresentation();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEnabled(true);
        user.setEmailVerified(false); // later to verify

        // Add custom attributes
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("phone", List.of(request.getPhone()));
        attributes.put("pays", List.of(request.getPays()));
        attributes.put("address", List.of(request.getAddress()));
        attributes.put("postalNumber", List.of(request.getPostalNumber()));
        attributes.put("birthDate", List.of(request.getBirthDate()));
        attributes.put("referralCode", List.of(request.getReferralCode()));
        user.setAttributes(attributes);

        UsersResource usersResource = keycloak.realm(realm).users();
        Response response = usersResource.create(user);

        if (response.getStatus() != 201) {
            throw new RuntimeException("Failed to create user: " + response.getStatusInfo());
        }

        // 3. Set password
        String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");

        CredentialRepresentation passwordCred = new CredentialRepresentation();
        passwordCred.setTemporary(false);
        passwordCred.setType(CredentialRepresentation.PASSWORD);
        passwordCred.setValue(request.getPassword());

        usersResource.get(userId).resetPassword(passwordCred);
        log.info("✅ Keycloak user created: {}", userId);
    }
}
