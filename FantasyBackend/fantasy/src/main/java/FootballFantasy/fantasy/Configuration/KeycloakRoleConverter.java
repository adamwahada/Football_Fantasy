package FootballFantasy.fantasy.Configuration;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.*;
import java.util.stream.Collectors;

public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        System.out.println("=== JWT CONVERTER DEBUG ===");
        System.out.println("JWT Subject: " + jwt.getSubject());
        System.out.println("JWT Claims: " + jwt.getClaims());

        Map<String, Object> realmAccess = (Map<String, Object>) jwt.getClaims().get("realm_access");
        System.out.println("Realm Access: " + realmAccess);

        if (realmAccess == null || realmAccess.get("roles") == null) {
            System.out.println("No roles found!");
            return Collections.emptyList();
        }

        List<String> roles = (List<String>) realmAccess.get("roles");
        System.out.println("Original roles: " + roles);

        Collection<GrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                .collect(Collectors.toList());

        System.out.println("Converted authorities: " + authorities);
        System.out.println("=== END JWT CONVERTER DEBUG ===");

        return authorities;
    }
}
