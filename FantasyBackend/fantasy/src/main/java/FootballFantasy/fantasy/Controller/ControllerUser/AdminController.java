package FootballFantasy.fantasy.Controller.ControllerUser;

import FootballFantasy.fantasy.Services.UserService.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UserService userService;

//    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/users/{userId}/credit")
    public ResponseEntity<String> credit(@PathVariable Long userId,
                                         @RequestParam BigDecimal amount) {
        userService.creditBalance(userId, amount);
        return ResponseEntity.ok("Balance updated");
    }
}