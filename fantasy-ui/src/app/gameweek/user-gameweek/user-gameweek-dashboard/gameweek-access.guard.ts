import { CanActivateFn, Router, ActivatedRouteSnapshot } from '@angular/router';
import { inject } from '@angular/core';
import { GameweekService } from '../../gameweek.service';
import { Observable, of, map } from 'rxjs';

const validCompetitions = [
  'PREMIER_LEAGUE',
  'SERIE_A',
  'CHAMPIONS_LEAGUE',
  'EUROPA_LEAGUE',
  'BUNDESLIGA',
  'LA_LIGA',
  'LIGUE_ONE',
  'BESTOFF',
  'CONFERENCE_LEAGUE',
];

export const gameweekAccessGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const router = inject(Router);
  const gameweekService = inject(GameweekService);

  const competition = route.paramMap.get('competition');
  const weekParam = route.paramMap.get('weekNumber');

  // Check competition validity first
  if (!competition || !validCompetitions.includes(competition)) {
    router.navigate(['/user/user-gameweek-list']);
    return false;
  }

  // If only competition param is present (no weekNumber), allow access
  if (!weekParam) {
    return true;
  }

  const weekNumber = Number(weekParam);
  if (isNaN(weekNumber)) {
    router.navigate(['/user/user-gameweek-list', competition]);
    return false;
  }

  // Validate week access via gameweek service
  return gameweekService.getAllGameweeksByCompetition(competition).pipe(
    map(gameweeks => {
      const requested = gameweeks.find(gw => gw.weekNumber === weekNumber);
      if (!requested) {
        router.navigate(['/user/user-gameweek-list', competition]);
        return false;
      }

      // La validation ne s'applique qu'aux gameweeks UPCOMING
      const start = new Date(requested.startDate);
      const nowCheck = new Date();
      const isUpcoming = nowCheck < start || (requested.status || '').toUpperCase() === 'UPCOMING';
      if (isUpcoming && !requested['validated']) {
        router.navigate(['/user/user-gameweek-list', competition]);
        return false;
      }
      const now = new Date();

      const sortedGameweeks = gameweeks.sort((a, b) => a.weekNumber - b.weekNumber);

      const currentGameweek = sortedGameweeks.find(gw => {
        const start = new Date(gw.startDate);
        const end = new Date(gw.endDate);
        return now >= start && now <= end;
      })?.weekNumber ?? sortedGameweeks.find(gw => new Date(gw.startDate) > now)?.weekNumber;

      if (currentGameweek === undefined) {
        router.navigate(['/user/user-gameweek-list', competition]);
        return false;
      }

      const allowedWeeks = [currentGameweek, currentGameweek + 1, currentGameweek + 2];

      if (allowedWeeks.includes(weekNumber)) {
        return true;
      } else {
        router.navigate(['/user/user-gameweek-list', competition]);
        return false;
      }
    })
  );
};
