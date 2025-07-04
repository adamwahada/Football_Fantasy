package FootballFantasy.fantasy.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test2")
public class Test2Controller {

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")  // ✅ Correct
    public ResponseEntity<String> bye() {
        return ResponseEntity.ok("Hello Admin from backend!");

    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")  // ✅ Correct
    public ResponseEntity<String> test44() {
        return ResponseEntity.ok("only accessible for admins!");

    }

    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ResponseEntity<String> hello() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("=== Authentication Info ===");
        System.out.println("Principal: " + auth.getPrincipal());
        System.out.println("Authorities: " + auth.getAuthorities());
        return ResponseEntity.ok("Hello user from backend!");
    }
}
