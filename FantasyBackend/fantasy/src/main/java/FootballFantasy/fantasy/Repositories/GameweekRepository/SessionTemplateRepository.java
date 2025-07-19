package FootballFantasy.fantasy.Repositories.GameweekRepository;

import FootballFantasy.fantasy.Entities.GameweekEntity.LeagueTheme;
import FootballFantasy.fantasy.Entities.GameweekEntity.SessionTemplate;
import FootballFantasy.fantasy.Entities.GameweekEntity.SessionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface SessionTemplateRepository extends JpaRepository<SessionTemplate, Long> {

    // Find template by session type and buy-in amount
    List<SessionTemplate> findBySessionTypeAndBuyInAmount(SessionType sessionType, BigDecimal buyInAmount);

    List<SessionTemplate> findByCompetitionAndSessionTypeAndIsActiveTrue(LeagueTheme competition, SessionType sessionType);


    // Find all active templates
    List<SessionTemplate> findByIsActiveTrue();

    // Find templates by session type
    List<SessionTemplate> findBySessionTypeAndIsActiveTrue(SessionType sessionType);
    @Modifying
    @Query("UPDATE SessionTemplate t SET t.isActive = :isActive WHERE t.id IN :ids")
    int updateTemplatesActiveStatus(@Param("ids") List<Long> ids, @Param("isActive") boolean isActive);

}