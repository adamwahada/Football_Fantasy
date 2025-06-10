package FootballFantasy.fantasy.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test3")
public class Test3Controller {

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")  // ✅ Correct
    public ResponseEntity<String> test44() {
        return ResponseEntity.ok("only accessible for admins!");

    }

}
