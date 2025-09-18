package FootballFantasy.fantasy.Exceptions.UsersExceptions;

import lombok.Getter;

@Getter
public class UserNotFoundException extends RuntimeException {
    private final String keycloakId;

    public UserNotFoundException(String keycloakId) {
        super(String.format("User not found for Keycloak ID: %s", keycloakId));
        this.keycloakId = keycloakId;
    }
}
