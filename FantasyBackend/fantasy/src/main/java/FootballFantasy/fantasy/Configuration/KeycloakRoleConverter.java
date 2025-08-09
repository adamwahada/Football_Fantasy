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


        Map<String, Object> realmAccess = (Map<String, Object>) jwt.getClaims().get("realm_access");

        if (realmAccess == null || realmAccess.get("roles") == null) {
            return Collections.emptyList();
        }

        List<String> roles = (List<String>) realmAccess.get("roles");

        Collection<GrantedAuthority> authorities = roles.stream()
                .map(role -> {
                    // Ne pas ajouter ROLE_ si déjà présent
                    String roleName = role.startsWith("ROLE_") ? role : "ROLE_" + role.toUpperCase();
                    return new SimpleGrantedAuthority(roleName);
                })
                .collect(Collectors.toList());


        return authorities;
    }
}
