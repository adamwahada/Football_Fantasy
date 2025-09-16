import { CanActivateFn, Router, ActivatedRouteSnapshot } from '@angular/router';
import { inject } from '@angular/core';
import { GameweekService } from '../../gameweek.service';
import { map } from 'rxjs';

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

export const finishedGameweekGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const router = inject(Router);
  const gameweekService = inject(GameweekService);

  const competition = route.paramMap.get('competition');
  const weekParam = route.paramMap.get('weekNumber');

  // Check competition validity
  if (!competition || !validCompetitions.includes(competition)) {
    router.navigate(['/user/user-gameweek-list']);
    return false;
  }

  // Week number is required for finished gameweeks
  if (!weekParam) {
    router.navigate(['/user/user-gameweek-list', competition]);
    return false;
  }

  const weekNumber = Number(weekParam);
  if (isNaN(weekNumber)) {
    router.navigate(['/user/user-gameweek-list', competition]);
    return false;
  }

  return gameweekService.getAllGameweeksByCompetition(competition).pipe(
    map(gameweeks => {
      const requested = gameweeks.find(gw => gw.weekNumber === weekNumber);
      if (!requested) {
        router.navigate(['/user/user-gameweek-list', competition]);
        return false;
      }

      // Check if gameweek is actually finished
      const now = new Date();
      const endDate = new Date(requested.endDate);
      const isFinished = now > endDate || (requested.status || '').toUpperCase() === 'FINISHED';

      if (!isFinished) {
        router.navigate(['/user/user-gameweek-list', competition]);
        return false;
      }

      return true;
    })
  );
};