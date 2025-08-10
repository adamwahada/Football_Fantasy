import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { GameweekService, Gameweek } from '../../../gameweek.service';
import { TeamService } from '../../../../match/team.service';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

export type LeagueTheme =
  | 'PREMIER_LEAGUE'
  | 'SERIE_A'
  | 'CHAMPIONS_LEAGUE'
  | 'EUROPE_LEAGUE'
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

@Component({
  selector: 'app-user-gameweek-details',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './user-gameweek-details.component.html',
  styleUrls: ['./user-gameweek-details.component.scss']
})
export class UserGameweekDetailsComponent implements OnInit, OnDestroy {
  competition!: string;
  gameweeks: Gameweek[] = [];
  loading = false;
  error = '';
  leagueIcons: { [key: string]: string } = {};
  currentGameweek: number | null = null;
  
  private destroy$ = new Subject<void>();

  // League configuration mapping
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
    'EUROPE_LEAGUE': { 
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
    // Load league icons first
    this.loadLeagueIcons();
    
    // Add modal-open class to body when component initializes (since this is a modal component)
    document.body.classList.add('modal-open');
    
    // Subscribe to route changes
    this.route.paramMap
      .pipe(takeUntil(this.destroy$))
      .subscribe(params => {
        this.competition = params.get('competition') || '';
        if (this.competition) {
          this.loadGameweeks(this.competition);
        }
      });
  }

  ngOnDestroy(): void {
    // Remove modal-open class when component is destroyed
    document.body.classList.remove('modal-open');
    
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Handle escape key to close modal
   */
  @HostListener('document:keydown.escape', ['$event'])
  onEscapeKey(event: KeyboardEvent): void {
    this.closeModal();
  }

  /**
   * Prevent modal content clicks from closing the modal
   */
  onModalContentClick(event: Event): void {
    event.stopPropagation();
  }

  /**
   * Handle backdrop click - only close if clicking directly on backdrop
   */
  onBackdropClick(event: Event): void {
    if (event.target === event.currentTarget) {
      this.closeModal();
    }
  }

  /**
   * Load all league icons from the service
   */
  private loadLeagueIcons(): void {
    this.teamService.getAllLeagueIcons()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (icons) => {
          this.leagueIcons = icons;
        },
        error: (err) => {
          console.warn('Failed to load league icons:', err);
          // Continue without icons
        }
      });
  }

  /**
   * Load gameweeks for the specified competition
   */
  loadGameweeks(competition: string): void {
    this.loading = true;
    this.error = '';
    
    this.gameweekService.getAllGameweeksByCompetition(competition)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          // Sort gameweeks by weekNumber ascending
          this.gameweeks = data.sort((a, b) => a.weekNumber - b.weekNumber);
          this.determineCurrentGameweek();
          this.loading = false;
        },
        error: (err) => {
          this.error = 'Failed to load gameweeks. Please try again.';
          this.loading = false;
          console.error('Error loading gameweeks:', err);
        }
      });
  }

  /**
   * Determine the current/active gameweek
   */
  private determineCurrentGameweek(): void {
    const now = new Date();
    
    // Find the gameweek that's currently active or next upcoming
    const activeGameweek = this.gameweeks.find(gw => {
      const startDate = new Date(gw.startDate);
      const endDate = new Date(gw.endDate);
      return now >= startDate && now <= endDate;
    });

    if (activeGameweek) {
      this.currentGameweek = activeGameweek.weekNumber;
    } else {
      // If no active gameweek, find the next upcoming one
      const upcomingGameweek = this.gameweeks.find(gw => {
        const startDate = new Date(gw.startDate);
        return now < startDate;
      });
      
      if (upcomingGameweek) {
        this.currentGameweek = upcomingGameweek.weekNumber;
      }
    }
  }

  /**
   * Format competition name for display
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
   * Get league icon URL for the current competition
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

  /**
   * Check if a gameweek is the current active one
   */
  isCurrentGameweek(weekNumber: number): boolean {
    return this.currentGameweek === weekNumber;
  }

  /**
   * Check if a gameweek is completed
   */
  isCompletedGameweek(gameweek: Gameweek): boolean {
    const now = new Date();
    const endDate = new Date(gameweek.endDate);
    return now > endDate;
  }

  /**
   * Check if a gameweek is upcoming
   */
  isUpcomingGameweek(gameweek: Gameweek): boolean {
    const now = new Date();
    const startDate = new Date(gameweek.startDate);
    return now < startDate;
  }

  /**
   * Get gameweek status for display
   */
  getGameweekStatus(gameweek: Gameweek): 'ONGOING' | 'FINISHED' | 'UPCOMING' | 'CANCELLED' {
    const now = new Date();
    const startDate = new Date(gameweek.startDate);
    const endDate = new Date(gameweek.endDate);

    if (now >= startDate && now <= endDate) {
      return 'ONGOING';
    } else if (now > endDate) {
      return 'FINISHED';
    } else {
      return 'UPCOMING';
    }
  }

  /**
   * Get formatted date range for gameweek
   */
  getGameweekDateRange(gameweek: Gameweek): string {
    const startDate = new Date(gameweek.startDate);
    const endDate = new Date(gameweek.endDate);
    
    const formatOptions: Intl.DateTimeFormatOptions = {
      month: 'short',
      day: 'numeric'
    };

    const start = startDate.toLocaleDateString('en-US', formatOptions);
    const end = endDate.toLocaleDateString('en-US', formatOptions);

    return `${start} - ${end}`;
  }

  /**
   * Close modal and navigate back
   */
  closeModal(): void {
    // Remove modal-open class before navigating
    document.body.classList.remove('modal-open');
    this.router.navigate(['user/user-gameweek-list']);
  }

  /**
   * Open gameweek matches
   */
  openGameweekMatches(weekNumber: number): void {
    // Add loading state to clicked gameweek
    const gameweek = this.gameweeks.find(gw => gw.weekNumber === weekNumber);
    if (gameweek) {
      // Remove modal-open class before navigating
      document.body.classList.remove('modal-open');
      this.router.navigate(['user/gameweek-matches', this.competition, weekNumber]);
    }
  }

  /**
   * Get CSS class for league theme
   */
  getLeagueThemeClass(): string {
    const config = this.leagueConfigs[this.competition as LeagueTheme];
    return config?.theme || 'default-theme';
  }

  /**
   * Track by function for gameweek list
   */
  trackByGameweek(index: number, gameweek: Gameweek): number {
    return gameweek.weekNumber;
  }

  /**
   * Retry loading gameweeks
   */
  retryLoading(): void {
    if (this.competition) {
      this.loadGameweeks(this.competition);
    }
  }

  /**
   * Check if gameweeks are available
   */
  hasGameweeks(): boolean {
    return this.gameweeks && this.gameweeks.length > 0;
  }

  /**
   * Get total number of gameweeks
   */
  getTotalGameweeks(): number {
    return this.gameweeks.length;
  }

  /**
   * Get completed gameweeks count
   */
  getCompletedCount(): number {
    return this.gameweeks.filter(gw => this.isCompletedGameweek(gw)).length;
  }

  /**
   * Get upcoming gameweeks count
   */
  getUpcomingCount(): number {
    return this.gameweeks.filter(gw => this.isUpcomingGameweek(gw)).length;
  }
}