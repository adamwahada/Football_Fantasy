package FootballFantasy.fantasy.Repositories.GameweekRepositories;

import FootballFantasy.fantasy.Entities.GameweekEntities.LeagueTheme;
import FootballFantasy.fantasy.Entities.GameweekEntities.SessionTemplate;
import FootballFantasy.fantasy.Entities.GameweekEntities.SessionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface SessionTemplateRepository extends JpaRepository<SessionTemplate, Long> {

    // ✅ ADD THIS MISSING METHOD - this is what your service is calling!
    Optional<SessionTemplate> findByCompetitionAndSessionTypeAndBuyInAmountAndIsActive(
            LeagueTheme competition,
            SessionType sessionType,
            BigDecimal buyInAmount,
            Boolean isActive);

    // ✅ ALTERNATIVE: More explicit method name that matches your usage pattern
    default Optional<SessionTemplate> findActiveTemplate(LeagueTheme competition,
                                                         SessionType sessionType,
                                                         BigDecimal buyInAmount) {
        return findByCompetitionAndSessionTypeAndBuyInAmountAndIsActive(
                competition, sessionType, buyInAmount, true);
    }

    // ✅ SAFE BigDecimal comparison using @Query (recommended for production)
    @Query("SELECT st FROM SessionTemplate st WHERE st.competition = :competition " +
            "AND st.sessionType = :sessionType " +
            "AND st.isActive = :isActive " +
            "AND st.buyInAmount = :buyInAmount")
    Optional<SessionTemplate> findTemplateWithBigDecimalSafe(@Param("competition") LeagueTheme competition,
                                                             @Param("sessionType") SessionType sessionType,
                                                             @Param("buyInAmount") BigDecimal buyInAmount,
                                                             @Param("isActive") Boolean isActive);

    // Your existing methods
    List<SessionTemplate> findBySessionTypeAndBuyInAmount(SessionType sessionType, BigDecimal buyInAmount);
    List<SessionTemplate> findByCompetitionAndSessionTypeAndIsActiveTrue(LeagueTheme competition, SessionType sessionType);
    List<SessionTemplate> findByIsActiveTrue();
    List<SessionTemplate> findBySessionTypeAndIsActiveTrue(SessionType sessionType);

    @Query("SELECT st FROM SessionTemplate st WHERE st.competition = :competition " +
            "AND st.sessionType = :sessionType " +
            "AND st.buyInAmount = :buyInAmount " +
            "AND st.isActive = true " +
            "AND st.isPrivate = :isPrivate")
    Optional<SessionTemplate> findActiveTemplateByCompetitionTypeAmountAndPrivacy(
            @Param("competition") LeagueTheme competition,
            @Param("sessionType") SessionType sessionType,
            @Param("buyInAmount") BigDecimal buyInAmount,
            @Param("isPrivate") Boolean isPrivate);

}