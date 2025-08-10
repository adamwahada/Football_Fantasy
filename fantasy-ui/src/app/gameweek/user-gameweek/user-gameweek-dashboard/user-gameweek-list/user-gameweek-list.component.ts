import { Component, OnInit, OnDestroy } from '@angular/core';
import { TeamService } from '../../../../match/team.service';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Subject, takeUntil, finalize } from 'rxjs';

interface League {
  displayName: string;
  iconUrl: string;
  competitionCode: string;
}

interface LeagueListItem {
  value: string;
  label: string;
  iconUrl: string;
}

@Component({
  selector: 'app-user-gameweek-list',
  templateUrl: './user-gameweek-list.component.html',
  styleUrls: ['./user-gameweek-list.component.scss'],
  imports: [CommonModule],
})
export class UserGameweekListComponent implements OnInit, OnDestroy {
  leagues: LeagueListItem[] = [];
  loading = true;
  error = '';

  private readonly destroy$ = new Subject<void>();

  private readonly competitions = [
    { value: 'PREMIER_LEAGUE', label: 'Premier League' },
    { value: 'LA_LIGA', label: 'La Liga' },
    { value: 'SERIE_A', label: 'Serie A' },
    { value: 'BUNDESLIGA', label: 'Bundesliga' },
    { value: 'LIGUE_ONE', label: 'Ligue 1' },
    { value: 'CHAMPIONS_LEAGUE', label: 'Champions League' },
    { value: 'EUROPA_LEAGUE', label: 'Europa League' },
  ];

  private readonly leagueSubtitles: Record<string, string> = {
    'PREMIER_LEAGUE': 'English Premier League',
    'LA_LIGA': 'Spanish La Liga',
    'SERIE_A': 'Italian Serie A',
    'BUNDESLIGA': 'German Bundesliga',
    'LIGUE_ONE': 'French Ligue 1',
    'CHAMPIONS_LEAGUE': 'UEFA Champions League',
    'EUROPA_LEAGUE': 'UEFA Europa League',
  };

  constructor(
    private teamService: TeamService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadLeagues();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Load leagues from the service
   */
  loadLeagues(): void {
    this.loading = true;
    this.error = '';

    this.teamService
      .getAllLeagueIcons()
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.loading = false;
        })
      )
      .subscribe({
        next: (leagueIconsMap) => {
          this.leagues = this.competitions.map(league => ({
            value: league.value,
            label: league.label,
            iconUrl: this.getLeagueIconUrl(leagueIconsMap, league.label),
          }));
        },
        error: (err) => {
          console.error('Failed to load leagues:', err);
          this.error = 'Failed to load leagues. Please try again later.';
          // Set default leagues with fallback icons
          this.leagues = this.competitions.map(league => ({
            value: league.value,
            label: league.label,
            iconUrl: '/assets/images/leagues/default.png',
          }));
        },
      });
  }

  /**
   * Handle league selection
   */
  onLeagueClick(competitionCode: string): void {
    if (!competitionCode) {
      console.error('Invalid competition code');
      return;
    }

    try {
      this.router.navigate(['user/user-gameweek-list', competitionCode]);
    } catch (error) {
      console.error('Navigation failed:', error);
    }
  }

  /**
   * Get league subtitle based on competition code
   */
  getLeagueSubtitle(competitionCode: string): string {
    return this.leagueSubtitles[competitionCode] || 'Football League';
  }

  /**
   * Track function for ngFor to improve performance
   */
  trackByLeague(index: number, league: LeagueListItem): string {
    return league.value;
  }

  /**
   * Handle image load errors
   */
  onImageError(event: Event): void {
    const img = event.target as HTMLImageElement;
    if (img) {
      img.src = '/assets/images/leagues/default.png';
    }
  }

  /**
   * Retry loading leagues
   */
  retryLoad(): void {
    this.loadLeagues();
  }

  /**
   * Get league icon URL with fallback
   */
  private getLeagueIconUrl(leagueIconsMap: Record<string, string>, leagueLabel: string): string {
    const iconPath = leagueIconsMap[leagueLabel];
    
    if (iconPath) {
      // Check if teamService has a convertToFullUrl method
      if (typeof this.teamService['convertToFullUrl'] === 'function') {
        return this.teamService['convertToFullUrl'](iconPath);
      }
      return iconPath;
    }
    
    return '/assets/images/leagues/default.png';
  }
}