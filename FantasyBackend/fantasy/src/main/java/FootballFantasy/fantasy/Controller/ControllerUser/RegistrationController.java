package FootballFantasy.fantasy.Controller.ControllerUser;


import FootballFantasy.fantasy.Dto.RegisterRequest;
import FootballFantasy.fantasy.Service.UserService.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        registrationService.registerUser(request);
        return ResponseEntity.ok(Map.of("message", "UserEntity registered successfully"));
    }
}
