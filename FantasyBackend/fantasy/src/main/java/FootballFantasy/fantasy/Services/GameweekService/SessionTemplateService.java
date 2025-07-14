package FootballFantasy.fantasy.Services.GameweekService;

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
    private SessionTemplateRepository sessionTemplateRepository;

    public SessionTemplate createTemplate(String templateName, SessionType sessionType,
                                          BigDecimal buyInAmount, Integer maxParticipants,
                                          String description) {
        SessionTemplate template = new SessionTemplate();
        template.setTemplateName(templateName);
        template.setSessionType(sessionType);
        template.setBuyInAmount(buyInAmount);
        template.setMaxParticipants(maxParticipants);
        template.setDescription(description);
        template.setIsActive(true);

        return sessionTemplateRepository.save(template);
    }

    public SessionTemplate updateTemplate(Long templateId, String templateName,
                                          BigDecimal buyInAmount, Integer maxParticipants,
                                          String description, boolean isActive, boolean isPrivate) {
        Optional<SessionTemplate> template = sessionTemplateRepository.findById(templateId);
        if (template.isPresent()) {
            SessionTemplate existing = template.get();
            existing.setTemplateName(templateName);
            existing.setBuyInAmount(buyInAmount);
            existing.setMaxParticipants(maxParticipants);
            existing.setIsPrivate(isPrivate);
            existing.setIsActive(isActive);
            existing.setDescription(description);
            return sessionTemplateRepository.save(existing);
        }
        throw new RuntimeException("Template not found");
    }

    public void deleteTemplate(Long templateId) {
        if (sessionTemplateRepository.existsById(templateId)) {
            sessionTemplateRepository.deleteById(templateId);
        } else {
            throw new RuntimeException("Template not found");
        }
    }

    public SessionTemplate toggleTemplate(Long templateId, boolean isActive) {
        Optional<SessionTemplate> template = sessionTemplateRepository.findById(templateId);
        if (template.isPresent()) {
            template.get().setIsActive(isActive);
            return sessionTemplateRepository.save(template.get());
        }
        throw new RuntimeException("Template not found");
    }

    public List<SessionTemplate> getTemplatesByType(SessionType sessionType) {
        return sessionTemplateRepository.findBySessionTypeAndIsActiveTrue(sessionType);
    }

    public List<SessionTemplate> getAllActiveTemplates() {
        return sessionTemplateRepository.findByIsActiveTrue();
    }

    public SessionTemplate findTemplate(SessionType sessionType, BigDecimal buyInAmount) {
        return sessionTemplateRepository.findBySessionTypeAndBuyInAmount(sessionType, buyInAmount)
                .stream()
                .filter(SessionTemplate::getIsActive)
                .findFirst()
                .orElse(null);
    }

    public List<SessionTemplate> findTemplates(SessionType sessionType, BigDecimal buyInAmount) {
        return sessionTemplateRepository.findBySessionTypeAndBuyInAmount(sessionType, buyInAmount);
    }
}
