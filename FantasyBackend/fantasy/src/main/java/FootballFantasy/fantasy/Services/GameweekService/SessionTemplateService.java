package FootballFantasy.fantasy.Services.GameweekService;

import FootballFantasy.fantasy.Entities.GameweekEntity.LeagueTheme;
import FootballFantasy.fantasy.Entities.GameweekEntity.SessionTemplate;
import FootballFantasy.fantasy.Entities.GameweekEntity.SessionType;
import FootballFantasy.fantasy.Repositories.GameweekRepository.SessionTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class SessionTemplateService {

    @Autowired
    private SessionTemplateRepository repo;

    public SessionTemplate createTemplate(LeagueTheme competition,
                                          String templateName,
                                          SessionType sessionType,
                                          BigDecimal buyInAmount,
                                          Integer maxParticipants,
                                          String description,
                                          boolean isPrivate) {
        SessionTemplate template = new SessionTemplate();
        template.setCompetition(competition);
        template.setTemplateName(templateName);
        template.setSessionType(sessionType);
        template.setBuyInAmount(buyInAmount);
        template.setMaxParticipants(maxParticipants);
        template.setIsPrivate(isPrivate);
        template.setIsActive(true);
        template.setDescription(description);
        return repo.save(template);
    }

    public SessionTemplate updateTemplate(Long id,
                                          LeagueTheme competition,
                                          String templateName,
                                          BigDecimal buyInAmount,
                                          Integer maxParticipants,
                                          String description,
                                          boolean isActive,
                                          boolean isPrivate) {
        SessionTemplate t = repo.findById(id).orElseThrow(() -> new RuntimeException("Template not found"));
        t.setCompetition(competition);
        t.setTemplateName(templateName);
        t.setBuyInAmount(buyInAmount);
        t.setMaxParticipants(maxParticipants);
        t.setIsPrivate(isPrivate);
        t.setIsActive(isActive);
        t.setDescription(description);
        return repo.save(t);
    }

    public void deleteTemplate(Long id) {
        if (!repo.existsById(id)) throw new RuntimeException("Template not found");
        repo.deleteById(id);
    }

    public SessionTemplate toggleTemplate(Long id, boolean isActive) {
        SessionTemplate t = repo.findById(id).orElseThrow(() -> new RuntimeException("Template not found"));
        t.setIsActive(isActive);
        return repo.save(t);
    }

    public List<SessionTemplate> getTemplates(LeagueTheme competition, SessionType sessionType) {
        return repo.findByCompetitionAndSessionTypeAndIsActiveTrue(competition, sessionType);
    }

    public List<SessionTemplate> getTemplatesByType(SessionType sessionType) {
        return repo.findBySessionTypeAndIsActiveTrue(sessionType);
    }

    public List<SessionTemplate> getAllActiveTemplates() {
        return repo.findByIsActiveTrue();
    }

    public List<SessionTemplate> findTemplates(SessionType sessionType, BigDecimal buyInAmount) {
        return repo.findBySessionTypeAndBuyInAmount(sessionType, buyInAmount);
    }
}
