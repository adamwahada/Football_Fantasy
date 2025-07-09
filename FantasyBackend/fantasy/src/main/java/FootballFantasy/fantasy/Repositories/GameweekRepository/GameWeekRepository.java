package FootballFantasy.fantasy.Repositories.GameweekRepository;

import FootballFantasy.fantasy.Entities.GameweekEntity.GameWeek;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameWeekRepository extends JpaRepository<GameWeek, Long> {
    Optional<GameWeek> findByWeekNumber(int weekNumber);

}