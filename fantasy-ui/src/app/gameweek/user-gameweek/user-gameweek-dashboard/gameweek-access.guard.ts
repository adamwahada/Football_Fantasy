import { CanActivateFn, Router, ActivatedRouteSnapshot } from '@angular/router';
import { inject } from '@angular/core';
import { GameweekService } from '../../gameweek.service';
import { Observable, map } from 'rxjs';

export const gameweekAccessGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const router = inject(Router);
  const gameweekService = inject(GameweekService);

  const competition = route.paramMap.get('competition');
  const weekNumber = Number(route.paramMap.get('weekNumber'));

  if (!competition || isNaN(weekNumber)) {
    router.navigate(['/user/user-gameweek-list']);
    return false;
  }

  // Get all gameweeks for this competition and check if requested week is allowed
  return gameweekService.getAllGameweeksByCompetition(competition).pipe(
    map(gameweeks => {
      // Find current active gameweek based on dates (similar to your component logic)
      const now = new Date();

      // Sort ascending just in case
      const sortedGameweeks = gameweeks.sort((a, b) => a.weekNumber - b.weekNumber);

      const currentGameweek = sortedGameweeks.find(gw => {
        const start = new Date(gw.startDate);
        const end = new Date(gw.endDate);
        return now >= start && now <= end;
      })?.weekNumber ?? sortedGameweeks.find(gw => new Date(gw.startDate) > now)?.weekNumber;

      if (currentGameweek === undefined) {
        // No current or upcoming gameweek found, block access
        router.navigate(['/user/user-gameweek-list']);
        return false;
      }

      // Allowed weeks: current + next 2
      const allowedWeeks = [currentGameweek, currentGameweek + 1, currentGameweek + 2];

      if (allowedWeeks.includes(weekNumber)) {
        return true;
      } else {
        router.navigate(['/user/user-gameweek-list']);
        return false;
      }
    })
  );
};
