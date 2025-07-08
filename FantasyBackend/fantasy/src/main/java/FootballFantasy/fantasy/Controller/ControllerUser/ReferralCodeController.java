package FootballFantasy.fantasy.Controller.ControllerUser;

import FootballFantasy.fantasy.Entities.UserEntity.ReferralCode;
import FootballFantasy.fantasy.Service.ServiceUser.ReferralCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/referral")
@RequiredArgsConstructor
public class ReferralCodeController {

    private final ReferralCodeService referralCodeService;

    // 1. Liste tous les codes
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<ReferralCode>> getAllReferralCodes() {
        List<ReferralCode> codes = referralCodeService.getAllReferralCodes();
        return ResponseEntity.ok(codes);
    }

    // 2. Crée un nouveau code
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ReferralCode> createReferralCode(
            @RequestBody ReferralCodeCreateRequest request) {
        ReferralCode created = referralCodeService.createReferralCode(request.getCode(), request.getExpirationDate());
        return ResponseEntity.ok(created);
    }

    // 3. Supprime un code par code (string)
    @DeleteMapping("/delete/{code}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteReferralCode(@PathVariable String code) {
        referralCodeService.deleteReferralCode(code);
        return ResponseEntity.noContent().build();
    }

    // DTO interne pour la requête création
    public static class ReferralCodeCreateRequest {
        private String code;

        // Format ISO yyyy-MM-dd attendu dans le JSON
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate expirationDate;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public LocalDate getExpirationDate() {
            return expirationDate;
        }

        public void setExpirationDate(LocalDate expirationDate) {
            this.expirationDate = expirationDate;
        }
    }
}
