package FootballFantasy.fantasy.Service.UserService;

import FootballFantasy.fantasy.Entities.UserEntity.ReferralCode;
import FootballFantasy.fantasy.Repositories.UserRepository.ReferralCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReferralCodeService {

    private final ReferralCodeRepository referralCodeRepository;

    public boolean isCodeValid(String code) {
        Optional<ReferralCode> optionalCode = referralCodeRepository.findByCode(code);
        return optionalCode.isPresent()
                && !optionalCode.get().isUsed()
                && (optionalCode.get().getExpirationDate() == null
                || optionalCode.get().getExpirationDate().isAfter(LocalDate.now()));
    }

    public void markCodeAsUsed(String code) {
        referralCodeRepository.findByCode(code).ifPresent(rc -> {
            rc.setUsed(true);
            referralCodeRepository.save(rc);
        });
    }
//crud cote admin pour gérer les codes
    public ReferralCode createReferralCode(String code, LocalDate expirationDate) {
        if (referralCodeRepository.findByCode(code).isPresent()) {
            throw new IllegalArgumentException("Code déjà existant");
        }
        ReferralCode newCode = new ReferralCode();
        newCode.setCode(code);
        newCode.setUsed(false);
        newCode.setExpirationDate(expirationDate);
        return referralCodeRepository.save(newCode);
    }

    public void deleteReferralCode(String code) {
        ReferralCode rc = referralCodeRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Code non trouvé"));
        referralCodeRepository.delete(rc);
    }

    public java.util.List<ReferralCode> getAllReferralCodes() {
        return referralCodeRepository.findAll();
    }
}
