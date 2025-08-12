// user-gameweek-matches.component.ts
import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, ParamMap } from '@angular/router';
import { Subject, forkJoin } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { GameweekService, Gameweek } from '../../../gameweek.service';
import { MatchWithIconsDTO } from '../../../../match/match.service';
import { TeamService, TeamIcon } from '../../../../match/team.service';
import { Router } from '@angular/router';

type PickOption = '1' | 'X' | '2' | null;

interface UIMatch extends MatchWithIconsDTO {
  pick?: PickOption;
  scoreHome?: number | null;
  scoreAway?: number | null;
  isTiebreak?: boolean;
  homeIconUrl?: string;  
  awayIconUrl?: string; 
}

@Component({
  selector: 'app-user-gameweek-matches',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './user-gameweek-matches.component.html',
  styleUrls: ['./user-gameweek-matches.component.scss']
})
export class UserGameweekMatchesComponent implements OnInit, OnDestroy {
  // route params displayed in the header/template
  selectedCompetition = '';
  selectedWeekNumber = 0;

  // UI data
  matches: UIMatch[] = [];
  loading = false;
  error: string | null = null;
  teamIconsMap: { [key: string]: string } = {};
  teamsWithIcons: TeamIcon[] = [];

  private destroy$ = new Subject<void>();

  constructor(
    private route: ActivatedRoute,
    private gameweekService: GameweekService,
    private teamService: TeamService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Load all team icons first
    this.teamService.getAllTeamIcons().subscribe({
      next: (icons) => {
        this.teamIconsMap = icons;
        // Build teamsWithIcons for lookup
        // Use all teams from the icon map
        this.teamsWithIcons = this.teamService.getTeamsWithIcons(Object.keys(icons), icons);
        // Now load the matches
        this.route.paramMap
          .pipe(takeUntil(this.destroy$))
          .subscribe((params: ParamMap) => {
            this.selectedCompetition = params.get('competition') || '';
            this.selectedWeekNumber = Number(params.get('weekNumber')) || 0;
            if (this.selectedCompetition && this.selectedWeekNumber > 0) {
              this.loadGameweekData();
            } else {
              this.error = 'Invalid gameweek parameters.';
              this.matches = [];
            }
          });
      },
      error: (err) => {
        console.error('Error loading team icons', err);
        // fallback: still load matches, but icons will be missing
        this.route.paramMap
          .pipe(takeUntil(this.destroy$))
          .subscribe((params: ParamMap) => {
            this.selectedCompetition = params.get('competition') || '';
            this.selectedWeekNumber = Number(params.get('weekNumber')) || 0;
            if (this.selectedCompetition && this.selectedWeekNumber > 0) {
              this.loadGameweekData();
            } else {
              this.error = 'Invalid gameweek parameters.';
              this.matches = [];
            }
          });
      }
    });
  }

  private loadGameweekData(): void {
    this.loading = true;
    this.error = null;
    this.matches = [];

    this.gameweekService.getAllGameweeksByCompetition(this.selectedCompetition)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (gameweeks: Gameweek[]) => {
          const gw = gameweeks.find(g => g.weekNumber === this.selectedWeekNumber);
          if (!gw || !gw.id) {
            this.error = 'Gameweek not found.';
            this.loading = false;
            return;
          }
          forkJoin({
            tiebreakers: this.gameweekService.getTiebreakerMatches(gw.id),
            matches: this.gameweekService.getMatchesWithIcons(gw.id)
          })
          .pipe(takeUntil(this.destroy$))
          .subscribe({
            next: ({ tiebreakers, matches }) => {
              const tbIds = (tiebreakers || []).map(m => m.id!).filter(Boolean);
              // Use TeamService.getTeamsWithIcons to get icon URLs for all teams in this match list
              const allTeams = Array.from(new Set((matches || []).flatMap(m => [m.homeTeam, m.awayTeam])));
              const teamsWithIcons = this.teamService.getTeamsWithIcons(allTeams, this.teamIconsMap);
              // Build a lookup for fast access
              const iconLookup: { [key: string]: string } = {};
              teamsWithIcons.forEach(ti => { iconLookup[ti.name] = ti.iconUrl; });

              this.matches = (matches || []).map((m: MatchWithIconsDTO) => ({
                ...m,
                pick: null,
                scoreHome: (m as any).scoreHome ?? null,
                scoreAway: (m as any).scoreAway ?? null,
                isTiebreak: tbIds.includes((m as any).id),
                homeIconUrl: iconLookup[m.homeTeam] || this.teamService.getDefaultIconPath(m.homeTeam),
                awayIconUrl: iconLookup[m.awayTeam] || this.teamService.getDefaultIconPath(m.awayTeam)
              })) as UIMatch[];
              this.loading = false;
            },
            error: (err) => {
              console.error('Error loading matches/tiebreakers', err);
              this.error = 'Failed to load matches or tiebreakers.';
              this.loading = false;
            }
          });
        },
        error: (err) => {
          console.error('Error loading gameweeks', err);
          this.error = 'Failed to load gameweeks.';
          this.loading = false;
        }
      });
  }

  // No longer needed: getIconUrl

  // toggle pick (1 / X / 2)
  selectPick(match: UIMatch, pick: PickOption): void {
    match.pick = match.pick === pick ? null : pick;
  }

  // reset picks & tie-break scores
  resetPredictions(): void {
    this.matches.forEach(m => {
      m.pick = null;
      if (m.isTiebreak) {
        m.scoreHome = null;
        m.scoreAway = null;
      }
    });
  }

  // basic client-side validations then emit or POST payload
  submitPredictions(): void {
    const missingPicks = this.matches.filter(m => !m.pick);
    if (missingPicks.length > 0) {
      const ids = missingPicks.map(m => (m as any).id ?? '?').join(', ');
      alert(`Veuillez choisir 1/X/2 pour tous les matches. Manquants: ${ids}`);
      return;
    }

    const invalidTiebreaks = this.matches
      .filter(m => m.isTiebreak)
      .filter(m => m.scoreHome === null || m.scoreAway === null);

    if (invalidTiebreaks.length > 0) {
      alert('Veuillez renseigner les scores pour les tie-breaks.');
      return;
    }

    // Prepare payload (adjust shape to your backend contract)
    const payload = this.matches.map(m => ({
      matchId: (m as any).id,
      pick: m.pick,
      scoreHome: m.isTiebreak ? m.scoreHome : null,
      scoreAway: m.isTiebreak ? m.scoreAway : null
    }));

    console.log('Submitting picks payload:', payload);
    alert('Choix soumis — voir console pour les détails (F12).');

    // TODO: call your backend endpoint to save the predictions if you have one:
    // this.myPredictionService.submitPredictions(payload).subscribe(...)
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
  get tiebreakCount(): number {
  return this.matches.filter(m => m.isTiebreak).length;
}
  cancelPredictions(): void {
    // Navigate back to the gameweek list
    this.router.navigate(['../user-gameweek-list'], { relativeTo: this.route });
  }
}
