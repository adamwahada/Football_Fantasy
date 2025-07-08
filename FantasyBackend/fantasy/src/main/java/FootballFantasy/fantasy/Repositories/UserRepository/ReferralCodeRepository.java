package FootballFantasy.fantasy.Repositories.UserRepository;


import FootballFantasy.fantasy.Entities.UserEntity.ReferralCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReferralCodeRepository extends JpaRepository<ReferralCode, Long> {

    Optional<ReferralCode> findByCode(String code);
}