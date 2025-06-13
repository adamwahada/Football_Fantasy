package com.example.keycloak;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;

public class OneSessionAuthenticator implements Authenticator {

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        // Get the authenticated user
        UserModel user = context.getUser();

        // If no user is authenticated yet, skip this authenticator
        if (user == null) {
            context.attempted();
            return;
        }

        // Remove other sessions for this user
        removeOtherUserSessions(context, user);

        // Continue the flow
        context.success();
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // This method is called when there's a form submission
        // For our use case, we don't need to handle form actions
        context.success();
    }

    private void removeOtherUserSessions(AuthenticationFlowContext context, UserModel user) {
        KeycloakSession session = context.getSession();
        RealmModel realm = context.getRealm();

        // Get current authentication session ID
        String currentAuthSessionId = context.getAuthenticationSession().getParentSession().getId();

        // Find and remove all other user sessions
        session.sessions().getUserSessionsStream(realm, user)
                .filter(userSession -> !userSession.getId().equals(currentAuthSessionId))
                .forEach(userSession -> {
                    session.sessions().removeUserSession(realm, userSession);
                });
    }

    @Override
    public boolean requiresUser() {
        return true; // This authenticator needs a user to be present
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true; // Always configured
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // No required actions needed
    }

    @Override
    public void close() {
        // No cleanup needed
    }
}