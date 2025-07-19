package FootballFantasy.fantasy.Services.DataService;


import FootballFantasy.fantasy.Entities.GameweekEntity.SessionTemplate;
import FootballFantasy.fantasy.Repositories.GameweekRepository.SessionTemplateRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SessionTemplateSeeder {

    private final SessionTemplateRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void seed() {
        try {
            if (repository.count() == 0) {
                InputStream inputStream = new ClassPathResource("data/session_templates.json").getInputStream();
                List<SessionTemplate> templates = objectMapper.readValue(inputStream, new TypeReference<>() {
                });
                repository.saveAll(templates);
                System.out.println("✅ Session templates seeded successfully.");
            } else {
                System.out.println("ℹ️ Templates already exist. Skipping seeding.");
            }
        } catch (Exception e) {
            System.err.println("❌ Error seeding session templates: " + e.getMessage());
        }
    }
}