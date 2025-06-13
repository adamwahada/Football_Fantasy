//package com.example.keycloak;
//
//import org.keycloak.events.Event;
//import org.keycloak.events.EventListenerProvider;
//import org.keycloak.events.EventType;
//import org.keycloak.events.admin.AdminEvent;
//import org.keycloak.models.KeycloakSession;
//import org.keycloak.models.RealmModel;
//import org.keycloak.models.UserModel;
//import org.keycloak.models.UserSessionModel;
//import org.jboss.logging.Logger;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//public class OneSessionEventListener implements EventListenerProvider {
//
//    private static final Logger logger = Logger.getLogger(OneSessionEventListener.class);
//    private final KeycloakSession session;
//
//    public OneSessionEventListener(KeycloakSession session) {
//        this.session = session;
//    }
//
//    @Override
//    public void onEvent(Event event) {
//        // Only process LOGIN events
//        if (event.getType() == EventType.LOGIN) {
//            logger.info("Login event detected for user: " + event.getUserId());
//            handleUserLogin(event);
//        }
//    }
//
//    @Override
//    public void onEvent(AdminEvent adminEvent, boolean b) {
//        // We don't need to handle admin events
//    }
//
//    private void handleUserLogin(Event event) {
//        try {
//            RealmModel realm = session.getContext().getRealm();
//            UserModel user = session.users().getUserById(realm, event.getUserId());
//
//            if (user == null) {
//                logger.warn("User not found for ID: " + event.getUserId());
//                return;
//            }
//
//            String currentSessionId = event.getSessionId();
//            logger.info("Current session ID: " + currentSessionId);
//
//            // Get all user sessions except the current one
//            List<UserSessionModel> sessionsToRemove = session.sessions()
//                    .getUserSessionsStream(realm, user)
//                    .filter(userSession -> !userSession.getId().equals(currentSessionId))
//                    .collect(Collectors.toList());
//
//            logger.info("Found " + sessionsToRemove.size() + " sessions to remove for user: " + user.getUsername());
//
//            // Remove old sessions
//            for (UserSessionModel sessionToRemove : sessionsToRemove) {
//                logger.info("Removing old session: " + sessionToRemove.getId());
//                session.sessions().removeUserSession(realm, sessionToRemove);
//            }
//
//            logger.info("One-session enforcement complete for user: " + user.getUsername());
//
//        } catch (Exception e) {
//            logger.error("Error in OneSessionEventListener", e);
//        }
//    }
//
//    @Override
//    public void close() {
//        // Cleanup if needed
//    }
//}