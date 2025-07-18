package FootballFantasy.fantasy.Events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class MatchCompletedEvent extends ApplicationEvent {
    private final Long matchId;

    public MatchCompletedEvent(Object source, Long matchId) {
        super(source);
        this.matchId = matchId;
    }
}
