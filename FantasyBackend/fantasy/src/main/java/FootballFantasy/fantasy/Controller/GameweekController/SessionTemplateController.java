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

    // ✅ Admin: Create a new Session Template
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

    // ✅ Admin: Update an existing template
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

    // ✅ Admin: Enable/disable a template
    @PutMapping("/{templateId}/toggle")
    public ResponseEntity<SessionTemplate> toggleTemplate(
            @PathVariable Long templateId,
            @RequestParam boolean isActive) {
        SessionTemplate updated = sessionTemplateService.toggleTemplate(templateId, isActive);
        return ResponseEntity.ok(updated);
    }

    // ✅ Admin: Delete a template
    @DeleteMapping("/{templateId}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long templateId) {
        sessionTemplateService.deleteTemplate(templateId);
        return ResponseEntity.noContent().build();
    }

    // ✅ Admin: Get templates by session type
    @GetMapping("/type/{sessionType}")
    public ResponseEntity<List<SessionTemplate>> getTemplatesByType(@PathVariable SessionType sessionType) {
        List<SessionTemplate> templates = sessionTemplateService.getTemplatesByType(sessionType);
        return ResponseEntity.ok(templates);
    }

    // ✅ User: Get all active templates
    @GetMapping("/active")
    public ResponseEntity<List<SessionTemplate>> getAllActiveTemplates() {
        List<SessionTemplate> templates = sessionTemplateService.getAllActiveTemplates();
        return ResponseEntity.ok(templates);
    }

    // ✅ User: Find a template by type + buyIn
    @GetMapping("/find")
    public ResponseEntity<SessionTemplate> findTemplate(
            @RequestParam SessionType sessionType,
            @RequestParam BigDecimal buyInAmount) {
        SessionTemplate template = sessionTemplateService.findTemplate(sessionType, buyInAmount);
        return ResponseEntity.ok(template);
    }

    // ✅ User: wants to play against his friend in a private session
//    @GetMapping("/private")
//    public ResponseEntity<SessionTemplate> findPrivateTemplate(
//            @RequestParam SessionType type,
//            @RequestParam BigDecimal buyIn,
//            @RequestParam String accessKey) {
//        SessionTemplate template = sessionTemplateService.findPrivateTemplate(type, buyIn, accessKey);
//        return ResponseEntity.ok(template);
//    }
}
