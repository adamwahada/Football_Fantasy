package FootballFantasy.fantasy.Entities.Chat;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

@Entity
public class Message {
    @Id
    @GeneratedValue
    private Long id;
    private String sender;
    private String content;
    private String type;
    private LocalDateTime timestamp;
}
