package FootballFantasy.fantasy.Controller.GameweekControllers;

import FootballFantasy.fantasy.Entities.GameweekEntities.LeagueTheme;
import FootballFantasy.fantasy.Entities.GameweekEntities.SessionTemplate;
import FootballFantasy.fantasy.Entities.GameweekEntities.SessionType;
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
    private SessionTemplateService service;

    @PostMapping
    public ResponseEntity<SessionTemplate> create(@RequestBody SessionTemplate template) {
        SessionTemplate saved = service.createTemplate(
                template.getCompetition(),
                template.getTemplateName(),
                template.getSessionType(),
                template.getBuyInAmount(),
                template.getMaxParticipants(),
                template.getDescription(),
                template.getIsPrivate()
        );
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<SessionTemplate> update(
            @PathVariable Long id,
            @RequestBody SessionTemplate t
    ) {
        SessionTemplate updated = service.updateTemplate(
                id,
                t.getCompetition(),
                t.getTemplateName(),
                t.getBuyInAmount(),
                t.getMaxParticipants(),
                t.getDescription(),
                t.getIsActive(),
                t.getIsPrivate()
        );
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/update-status")
    public ResponseEntity<List<SessionTemplate>> updateTemplatesStatus(
            @RequestParam List<Long> ids,
            @RequestParam boolean isActive) {
        List<SessionTemplate> updated = service.updateTemplateStatus(ids, isActive);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteTemplate(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-competition/{competition}/type/{sessionType}")
    public ResponseEntity<List<SessionTemplate>> getByCompAndType(
            @PathVariable LeagueTheme competition,
            @PathVariable SessionType sessionType
    ) {
        return ResponseEntity.ok(service.getTemplates(competition, sessionType));
    }

    @GetMapping("/by-type/{sessionType}")
    public ResponseEntity<List<SessionTemplate>> getByType(@PathVariable SessionType sessionType) {
        return ResponseEntity.ok(service.getTemplatesByType(sessionType));
    }

    @GetMapping("/active")
    public ResponseEntity<List<SessionTemplate>> getAllActive() {
        return ResponseEntity.ok(service.getAllActiveTemplates());
    }

    @GetMapping("/find")
    public ResponseEntity<List<SessionTemplate>> find(
            @RequestParam SessionType sessionType,
            @RequestParam BigDecimal buyInAmount
    ) {
        List<SessionTemplate> found = service.findTemplates(sessionType, buyInAmount);
        return found.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(found);
    }
}

