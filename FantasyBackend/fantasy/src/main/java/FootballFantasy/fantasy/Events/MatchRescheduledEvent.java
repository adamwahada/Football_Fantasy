package FootballFantasy.fantasy.Events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class MatchRescheduledEvent extends ApplicationEvent {
    private final Long matchId;

    public MatchRescheduledEvent(Object source, Long matchId) {
        super(source);
        this.matchId = matchId;
    }
}