package FootballFantasy.fantasy.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@Component
public class RealtimeTokenValidationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RealtimeTokenValidationFilter.class);

    @Value("${keycloak.auth-server-url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.backend.client-id}")
    private String backendClientId;

    @Value("${keycloak.backend.client-secret}")
    private String backendClientSecret;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void debugConfig() {
        logger.info("=== REALTIME FILTER CONFIGURATION DEBUG ===");
        logger.info("Keycloak URL: {}", keycloakUrl);
        logger.info("Realm: {}", realm);
        logger.info("Backend Client ID: {}", backendClientId);
        logger.info("Backend Client Secret: {}", backendClientSecret != null ? "SET (length: " + backendClientSecret.length() + ")" : "NULL");
        logger.info("===============================================");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        logger.info("=== REALTIME TOKEN VALIDATION FILTER DEBUG ===");
        logger.info("Request URI: {}", request.getRequestURI());
        logger.info("Request Method: {}", request.getMethod());

        String authHeader = request.getHeader("Authorization");
        logger.info("Authorization Header: {}", authHeader != null ? "Present" : "Missing");

        // TEMPORARY: Skip validation for testing
        logger.info("=== FILTER TEMPORARILY DISABLED FOR TESTING ===");
        filterChain.doFilter(request, response);
        return;

        /*
        // Skip validation for public endpoints
        String requestURI = request.getRequestURI();
        if (isPublicEndpoint(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (!isTokenValid(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Token invalid or session terminated\"}");
            return;
        }

        filterChain.doFilter(request, response);
        */
    }

    private boolean isPublicEndpoint(String uri) {
        return uri.contains("/swagger") ||
                uri.contains("/v3/api-docs") ||
                uri.contains("/webjars") ||
                uri.contains("/configuration") ||
                uri.contains("/swagger-resources");
    }

    private boolean isTokenValid(String token) {
        try {
            logger.info("=== TOKEN VALIDATION DEBUG START ===");
            logger.info("Keycloak URL: {}", keycloakUrl);
            logger.info("Realm: {}", realm);
            logger.info("Backend Client ID: {}", backendClientId);
            logger.info("Backend Client Secret: {}", backendClientSecret != null ? "SET" : "NULL");

            // First, get an access token using client credentials
            String tokenUrl = keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token";
            logger.info("Token URL: {}", tokenUrl);

            HttpHeaders tokenHeaders = new HttpHeaders();
            tokenHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> tokenBody = new LinkedMultiValueMap<>();
            tokenBody.add("grant_type", "client_credentials");
            tokenBody.add("client_id", backendClientId);
            tokenBody.add("client_secret", backendClientSecret);

            HttpEntity<MultiValueMap<String, String>> tokenEntity = new HttpEntity<>(tokenBody, tokenHeaders);

            logger.info("Requesting client credentials token...");
            ResponseEntity<Map> tokenResponse = restTemplate.postForEntity(tokenUrl, tokenEntity, Map.class);

            logger.info("Token response status: {}", tokenResponse.getStatusCode());
            logger.info("Token response body: {}", tokenResponse.getBody());

            if (tokenResponse.getStatusCode() != HttpStatus.OK) {
                logger.error("Failed to get client credentials token. Status: {}", tokenResponse.getStatusCode());
                return false;
            }

            Map responseBody = tokenResponse.getBody();
            if (responseBody == null || !responseBody.containsKey("access_token")) {
                logger.error("No access_token in response: {}", responseBody);
                return false;
            }

            String clientAccessToken = (String) responseBody.get("access_token");
            logger.info("Client access token obtained: {}", clientAccessToken != null ? "YES" : "NO");

            // Now use this access token to introspect the user token
            String introspectUrl = keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token/introspect";
            logger.info("Introspect URL: {}", introspectUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBearerAuth(clientAccessToken);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("token", token);
            body.add("token_type_hint", "access_token");

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

            logger.info("Sending introspection request...");
            ResponseEntity<String> introspectionResponse = restTemplate.postForEntity(introspectUrl, entity, String.class);

            logger.info("Introspection response status: {}", introspectionResponse.getStatusCode());
            logger.info("Introspection response body: {}", introspectionResponse.getBody());

            if (introspectionResponse.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> introspectionResult = objectMapper.readValue(introspectionResponse.getBody(), Map.class);
                boolean isActive = Boolean.TRUE.equals(introspectionResult.get("active"));
                logger.info("Token active: {}", isActive);
                logger.info("=== TOKEN VALIDATION DEBUG END ===");
                return isActive;
            }

            logger.error("Introspection failed with status: {}", introspectionResponse.getStatusCode());
            return false;

        } catch (Exception e) {
            logger.error("Token validation failed with exception", e);
            logger.info("=== TOKEN VALIDATION DEBUG END (EXCEPTION) ===");
            return false;
        }
    }
}