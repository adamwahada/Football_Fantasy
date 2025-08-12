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

export type LeagueTheme =
  | 'PREMIER_LEAGUE'
  | 'SERIE_A'
  | 'CHAMPIONS_LEAGUE'
  | 'EUROPA_LEAGUE'
  | 'BUNDESLIGA'
  | 'LA_LIGA'
  | 'LIGUE_ONE'
  | 'BESTOFF'
  | 'CONFERENCE_LEAGUE';

interface LeagueConfig {
  displayName: string;
  iconKey: string;
  theme: string;
}

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

  // Deadline countdown
  deadline: Date | null = null;
  deadlineCountdown: string = '';
  private countdownInterval: any;

  private destroy$ = new Subject<void>();

  leagueDisplayName: string = '';
  leagueIconUrl: string = '';
  leagueIcons: { [key: string]: string } = {};

  // League configuration mapping (same as user-gameweek-details component)
  private leagueConfigs: Record<LeagueTheme, LeagueConfig> = {
    'PREMIER_LEAGUE': { 
      displayName: 'Premier League', 
      iconKey: 'Premier League',
      theme: 'premier-league'
    },
    'LA_LIGA': { 
      displayName: 'La Liga', 
      iconKey: 'La Liga',
      theme: 'la-liga'
    },
    'SERIE_A': { 
      displayName: 'Serie A', 
      iconKey: 'Serie A',
      theme: 'serie-a'
    },
    'BUNDESLIGA': { 
      displayName: 'Bundesliga', 
      iconKey: 'Bundesliga',
      theme: 'bundesliga'
    },
    'LIGUE_ONE': { 
      displayName: 'Ligue 1', 
      iconKey: 'Ligue 1',
      theme: 'ligue-1'
    },
    'CHAMPIONS_LEAGUE': { 
      displayName: 'Champions League', 
      iconKey: 'Champions League',
      theme: 'champions-league'
    },
    'EUROPA_LEAGUE': { 
      displayName: 'Europa League', 
      iconKey: 'Europa League',
      theme: 'europa-league'
    },
    'CONFERENCE_LEAGUE': { 
      displayName: 'Conference League', 
      iconKey: 'Conference League',
      theme: 'conference-league'
    },
    'BESTOFF': { 
      displayName: 'Best Of', 
      iconKey: 'Best Of',
      theme: 'best-of'
    }
  };

  constructor(
    private route: ActivatedRoute,
    private gameweekService: GameweekService,
    private teamService: TeamService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Load all league icons first (like in details)
    this.teamService.getAllLeagueIcons().subscribe({
      next: (icons) => {
        this.leagueIcons = icons;
        // Now load all team icons for matches
        this.teamService.getAllTeamIcons().subscribe({
          next: (teamIcons) => {
            this.teamIconsMap = teamIcons;
            this.teamsWithIcons = this.teamService.getTeamsWithIcons(Object.keys(teamIcons), teamIcons);
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
          error: () => {
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
      },
      error: () => {
        // fallback: still load matches, but league icons will be missing
        this.teamService.getAllTeamIcons().subscribe({
          next: (teamIcons) => {
            this.teamIconsMap = teamIcons;
            this.teamsWithIcons = this.teamService.getTeamsWithIcons(Object.keys(teamIcons), teamIcons);
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
          // Set deadline and start countdown
          this.deadline = gw.joinDeadline ? new Date(gw.joinDeadline) : null;
          this.updateDeadlineCountdown();
          if (this.countdownInterval) {
            clearInterval(this.countdownInterval);
          }
          this.countdownInterval = setInterval(() => this.updateDeadlineCountdown(), 1000);

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

              this.matches = (matches || [])
                .filter((m: MatchWithIconsDTO) =>
                   m.active !== false 
                )
                .map((m: MatchWithIconsDTO) => ({
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
    
    // Set league display name and icon using the same logic as user-gameweek-details
    this.leagueDisplayName = this.formatCompetitionName(this.selectedCompetition);
    this.leagueIconUrl = this.getLeagueIconUrl(this.selectedCompetition);
  }

  private updateDeadlineCountdown(): void {
    if (!this.deadline) {
      this.deadlineCountdown = '';
      return;
    }
    const now = new Date();
    let diffMs = this.deadline.getTime() - now.getTime();
    if (diffMs <= 0) {
      this.deadlineCountdown = 'Deadline passed';
      return;
    }
    const days = Math.floor(diffMs / (1000 * 60 * 60 * 24));
    diffMs -= days * (1000 * 60 * 60 * 24);
    const hours = Math.floor(diffMs / (1000 * 60 * 60));
    diffMs -= hours * (1000 * 60 * 60);
    const minutes = Math.floor(diffMs / (1000 * 60));
    // Only show non-zero units for compactness
    const parts = [];
    if (days > 0) parts.push(`${days} day${days > 1 ? 's' : ''}`);
    if (hours > 0 || days > 0) parts.push(`${hours} hour${hours > 1 ? 's' : ''}`);
    parts.push(`${minutes} minute${minutes > 1 ? 's' : ''}`);
    this.deadlineCountdown = parts.join(' ');
  }

  /**
   * Format competition name for display (same as user-gameweek-details)
   */
  formatCompetitionName(enumName: string): string {
    const config = this.leagueConfigs[enumName as LeagueTheme];
    return config?.displayName || enumName
      .toLowerCase()
      .split('_')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  }

  /**
   * Get league icon URL for the current competition (same as user-gameweek-details)
   */
  getLeagueIconUrl(competition: string): string {
    const config = this.leagueConfigs[competition as LeagueTheme];
    if (!config || !this.leagueIcons[config.iconKey]) {
      return this.teamService.getLeagueIconUrl('/assets/images/leagues/default.png');
    }

    return this.teamService.getLeagueIconUrl(this.leagueIcons[config.iconKey]);
  }

  /**
   * Handle image loading errors
   */
  handleImageError(event: Event): void {
    const imgElement = event.target as HTMLImageElement;
    imgElement.src = this.teamService.getLeagueIconUrl('/assets/images/leagues/default.png');
    
    // Add error class for styling
    imgElement.classList.add('image-error');
  }

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
    if (this.countdownInterval) {
      clearInterval(this.countdownInterval);
    }
  }

  get tiebreakCount(): number {
    return this.matches.filter(m => m.isTiebreak).length;
  }

  cancelPredictions(): void {
    // Navigate back to the gameweek list
    this.router.navigate(['../user-gameweek-list'], { relativeTo: this.route });
  }

}