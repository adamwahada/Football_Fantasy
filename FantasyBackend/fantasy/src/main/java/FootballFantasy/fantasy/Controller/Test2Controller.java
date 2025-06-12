package FootballFantasy.fantasy.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

}
