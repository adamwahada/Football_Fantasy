package FootballFantasy.fantasy.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Football Fantasy API")
                        .version("1.0.0")
                        .description("Documentation pour l’API sécurisée avec Keycloak"))
                .addSecurityItem(new SecurityRequirement().addList("keycloak", List.of("profile", "email")))
                .components(new Components()
                        .addSecuritySchemes("keycloak", new SecurityScheme()
                                .type(SecurityScheme.Type.OAUTH2)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .flows(new OAuthFlows()
                                        .authorizationCode(new OAuthFlow()
                                                .authorizationUrl("http://localhost:8180/realms/football-fantasy/protocol/openid-connect/auth")
                                                .tokenUrl("http://localhost:8180/realms/football-fantasy/protocol/openid-connect/token")
                                                .scopes(new Scopes()
                                                        .addString("profile", "User profile")
                                                        .addString("email", "User email")
                                                )
                                        )
                                )
                        )
                );
    }
}
