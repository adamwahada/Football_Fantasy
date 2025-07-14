package FootballFantasy.fantasy.Controller.GameweekController;

import FootballFantasy.fantasy.Entities.GameweekEntity.SessionTemplate;
import FootballFantasy.fantasy.Entities.GameweekEntity.SessionType;
import FootballFantasy.fantasy.Services.GameweekService.SessionTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/session-templates")
public class SessionTemplateController {

    @Autowired
    private SessionTemplateService sessionTemplateService;

    @PostMapping
    public ResponseEntity<SessionTemplate> createTemplate(@RequestBody SessionTemplate template) {
        SessionTemplate saved = sessionTemplateService.createTemplate(
                template.getTemplateName(),
                template.getSessionType(),
                template.getBuyInAmount(),
                template.getMaxParticipants(),
                template.getDescription()
        );
        return ResponseEntity.ok(saved);
    }

    @PutMapping("update/{templateId}")
    public ResponseEntity<SessionTemplate> updateTemplate(
            @PathVariable Long templateId,
            @RequestBody SessionTemplate template) {
        SessionTemplate updated = sessionTemplateService.updateTemplate(
                templateId,
                template.getTemplateName(),
                template.getBuyInAmount(),
                template.getMaxParticipants(),
                template.getDescription(),
                template.getIsActive(),
                template.getIsPrivate()
        );
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/{templateId}/toggle")
    public ResponseEntity<SessionTemplate> toggleTemplate(
            @PathVariable Long templateId,
            @RequestParam boolean isActive) {
        SessionTemplate updated = sessionTemplateService.toggleTemplate(templateId, isActive);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{templateId}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long templateId) {
        sessionTemplateService.deleteTemplate(templateId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/type/{sessionType}")
    public ResponseEntity<List<SessionTemplate>> getTemplatesByType(@PathVariable SessionType sessionType) {
        List<SessionTemplate> templates = sessionTemplateService.getTemplatesByType(sessionType);
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/active")
    public ResponseEntity<List<SessionTemplate>> getAllActiveTemplates() {
        List<SessionTemplate> templates = sessionTemplateService.getAllActiveTemplates();
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/find")
    public ResponseEntity<List<SessionTemplate>> findTemplates(
            @RequestParam SessionType sessionType,
            @RequestParam BigDecimal buyInAmount) {

        List<SessionTemplate> templates = sessionTemplateService.findTemplates(sessionType, buyInAmount);

        if (templates.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(templates);
    }
}
