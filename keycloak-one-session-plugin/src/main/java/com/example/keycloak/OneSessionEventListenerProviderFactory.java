//package com.example.keycloak;
//
//import org.keycloak.Config;
//import org.keycloak.events.EventListenerProvider;
//import org.keycloak.events.EventListenerProviderFactory;
//import org.keycloak.models.KeycloakSession;
//import org.keycloak.models.KeycloakSessionFactory;
//
//public class OneSessionEventListenerProviderFactory implements EventListenerProviderFactory {
//
//    public static final String ID = "one-session-event-listener";
//
//    @Override
//    public EventListenerProvider create(KeycloakSession session) {
//        return new OneSessionEventListener(session);
//    }
//
//    @Override
//    public void init(Config.Scope config) {
//        // Initialization if needed
//    }
//
//    @Override
//    public void postInit(KeycloakSessionFactory factory) {
//        // Post-initialization if needed
//    }
//
//    @Override
//    public void close() {
//        // Cleanup if needed
//    }
//
//    @Override
//    public String getId() {
//        return ID;
//    }
//}