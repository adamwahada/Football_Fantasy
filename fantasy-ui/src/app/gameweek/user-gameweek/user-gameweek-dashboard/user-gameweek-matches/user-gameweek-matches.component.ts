// user-gameweek-matches.component.ts
import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, ParamMap } from '@angular/router';
import { Subject, forkJoin, combineLatest, of } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { GameweekService, Gameweek } from '../../../gameweek.service';
import { MatchWithIconsDTO } from '../../../../match/match.service';
import { TeamService, TeamIcon } from '../../../../match/team.service';
import { SessionParticipationData, PredictionPayload, UserGameweekParticipationModalComponent } from '../user-gameweek-participation-modal/user-gameweek-participation-modal.component';
import { SessionParticipationService } from '../../../session-participation.service';
import { Router } from '@angular/router';
import { PredictionService, PredictionDTO, GameweekPredictionSubmissionDTO } from '../../../prediction.service';

type PickOption = 'HOME_WIN' | 'DRAW' | 'AWAY_WIN' | null;

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
  scoreHome: number | null; 
  scoreAway: number | null;
  isTiebreak?: boolean;
  homeIconUrl?: string;  
  awayIconUrl?: string; 
}

// Use consistent number values (no decimals in the constant)
export const PRECONFIGURED_BUY_IN_AMOUNTS = [10, 20, 50, 100] as const;
export type BuyInAmount = typeof PRECONFIGURED_BUY_IN_AMOUNTS[number];

@Component({
  selector: 'app-user-gameweek-matches',
  standalone: true,
  imports: [CommonModule, FormsModule, UserGameweekParticipationModalComponent],
  templateUrl: './user-gameweek-matches.component.html',
  styleUrls: ['./user-gameweek-matches.component.scss']
})
export class UserGameweekMatchesComponent implements OnInit, OnDestroy {
  // route params displayed in the header/template
  selectedCompetition!: LeagueTheme;
  selectedWeekNumber = 0;

  //prediction data
  showParticipationModal = false;
  preparedPredictions: PredictionPayload[] = [];
  isSubmitting = false;
  message = '';
  messageType: 'success' | 'error' = 'success';
  showMessage = false;

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
    private router: Router,
    private sessionParticipationService: SessionParticipationService,
    private predictionService: PredictionService 
  ) {}

  ngOnInit(): void {
    // Load all league and team icons in parallel, then handle route params
    const leagueIcons$ = this.teamService.getAllLeagueIcons();
    const teamIcons$ = this.teamService.getAllTeamIcons();

    combineLatest([leagueIcons$, teamIcons$, this.route.paramMap])
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: ([leagueIcons, teamIcons, params]) => {
          this.leagueIcons = leagueIcons || {};
          this.teamIconsMap = teamIcons || {};
          this.teamsWithIcons = this.teamService.getTeamsWithIcons(Object.keys(this.teamIconsMap), this.teamIconsMap);

          this.selectedCompetition = (params.get('competition') as LeagueTheme) || '' as LeagueTheme;
          this.selectedWeekNumber = Number(params.get('weekNumber')) || 0;

          if (this.selectedCompetition && this.selectedWeekNumber > 0) {
            this.loadGameweekData();
          } else {
            this.error = 'Invalid gameweek parameters.';
            this.matches = [];
          }
        },
        error: () => {
          this.error = 'Failed to load icons or route parameters.';
          this.matches = [];
        }
      });
  }

  // When you load the gameweek, store its ID:
  private currentGameweekId: number | null = null;

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
          this.currentGameweekId = gw.id; // <-- Store the real ID
          console.log('[GAMEWEEK] Loaded gameweek:', gw); // <-- Add this log
          console.log('[GAMEWEEK] Loaded gameweekId:', gw.id, 'for weekNumber:', gw.weekNumber, 'competition:', gw.competition); // <-- Add this log
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
    // Validate picks
    const missingPicks = this.matches.filter(m => !m.pick);
    if (missingPicks.length > 0) {
      const ids = missingPicks.map(m => (m as any).id ?? '?').join(', ');
      this.showErrorMessage(`Veuillez choisir 1/X/2 pour tous les matches. Manquants: ${ids}`);
      return;
    }

    const invalidTiebreaks = this.matches
      .filter(m => m.isTiebreak)
      .filter(m => m.scoreHome === null || m.scoreAway === null);

    if (invalidTiebreaks.length > 0) {
      this.showErrorMessage('Veuillez renseigner les scores pour les tie-breaks.');
      return;
    }

    // Ensure selectedCompetition is a valid LeagueTheme
    if (!this.selectedCompetition || typeof this.selectedCompetition !== 'string' || !(this.selectedCompetition in this.leagueConfigs)) {
      this.showErrorMessage('Competition is not valid. Please reload the page.');
      return;
    }

    // Prepare predictions payload
    this.preparedPredictions = this.matches
      .filter(m => m && typeof (m as any).id !== 'undefined' && !isNaN(Number((m as any).id)))
      .map(m => ({
        matchId: Number((m as any).id),
        pick: m.pick || '',
        scoreHome: m.isTiebreak ? (m.scoreHome ?? null) : null,
        scoreAway: m.isTiebreak ? (m.scoreAway ?? null) : null
      }));

    console.log('Prepared predictions:', this.preparedPredictions);

    // Show modal for session participation
    this.showParticipationModal = true;
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
onParticipationSubmitted(event: {
  sessionData: SessionParticipationData;
  predictions: PredictionPayload[];
}): void {
  console.log('[PARENT] Received participation event:', event);

  this.isSubmitting = true;
  const { sessionData, predictions } = event;

  // Use the real Gameweek ID loaded from backend, not the one from sessionData
  const gameweekIdToSend = this.currentGameweekId ?? sessionData.gameweekId;
  console.log('[PARENT] Using gameweekId for submission:', gameweekIdToSend);

  // Ensure buyInAmount is a clean number
  const cleanBuyInAmount = Number(sessionData.buyInAmount);
  
  // Validate essential data
  if (!sessionData.gameweekId || !sessionData.competition || !sessionData.sessionType) {
    console.error('[PARENT] Missing essential session data');
    this.showErrorMessage('Données de session manquantes. Veuillez réessayer.');
    this.isSubmitting = false;
    return;
  }

  if (isNaN(cleanBuyInAmount) || cleanBuyInAmount < 0) {
    console.error('[PARENT] Invalid buyInAmount:', sessionData.buyInAmount);
    this.showErrorMessage('Montant de mise invalide. Veuillez sélectionner un montant valide.');
    this.isSubmitting = false;
    return;
  }

  // Convert predictions to the required DTO format
  const predictionDTOs: PredictionDTO[] = predictions.map(p => ({
    matchId: p.matchId,
    predictedResult: this.mapPickToResult(p.pick),
    predictedHomeScore: p.scoreHome,
    predictedAwayScore: p.scoreAway
  }));

  // ✅ CREATE CLEAN REQUEST BODY (no session fields)
  const submissionDTO: GameweekPredictionSubmissionDTO = {
    userId: this.getCurrentUserId(),
    gameweekId: gameweekIdToSend, // <-- always use the backend ID
    competition: sessionData.competition,
    predictions: predictionDTOs
  };

  console.log('[PARENT] Final submission DTO:', submissionDTO);
  console.log('[PARENT] Session params - sessionType:', sessionData.sessionType, 'buyInAmount:', Number(sessionData.buyInAmount));

  // ✅ PASS SESSION DATA AS SEPARATE PARAMETERS
  this.predictionService.submitPredictionsAndJoinSession(
    submissionDTO,              // Clean request body
    sessionData.sessionType,    // Query param
    Number(sessionData.buyInAmount), // Query param
    sessionData.isPrivate,     // Query param
    sessionData.accessKey      // Query param (optional)
  ).subscribe({
    next: (result) => {
      console.log('[PARENT] Submission successful:', result);
      this.showSuccessMessage('Prédictions soumises avec succès!');
      this.showParticipationModal = false;
      this.isSubmitting = false;
    },
    error: (error) => {
      console.error('[PARENT] Submission error:', error);
      this.isSubmitting = false;
      this.showErrorMessage(this.getErrorMessage(error));
    }
  });
}

  // Helper to map pick to backend result
  private mapPickToResult(pick: string | null): 'HOME_WIN' | 'DRAW' | 'AWAY_WIN' {
    if (pick === 'HOME_WIN' || pick === '1') return 'HOME_WIN';
    if (pick === 'AWAY_WIN' || pick === '2') return 'AWAY_WIN';
    return 'DRAW'; // Default to DRAW for 'X' or null
  }

  // Implement this to get the logged-in user's ID
  private getCurrentUserId(): number {
    // TODO: Replace with actual user service implementation
    // Example: return this.authService.getCurrentUser()?.id || 1;
    return 1; // placeholder
  }

  private getErrorMessage(error: any): string {
    if (!error) return 'Erreur lors de la soumission. Veuillez réessayer.';
    
    if (error.status === 409) {
      return 'Vous participez déjà à cette session ou la session est pleine.';
    }
    
    if (error.status === 400) {
      return 'Paramètres invalides. Vérifiez vos données.';
    }
    
    if (error.status === 500) {
      return 'Erreur serveur. Veuillez réessayer plus tard.';
    }
    
    if (error.error && typeof error.error === 'string') {
      return error.error;
    }
    
    if (error.message) {
      return error.message;
    }
    
    return 'Erreur lors de la soumission. Veuillez réessayer.';
  }

  onModalClosed(): void {
    console.log('[PARENT] Modal closed');
    this.showParticipationModal = false;
  }

  private showSuccessMessage(msg: string): void {
    this.message = msg;
    this.messageType = 'success';
    this.showMessage = true;
    setTimeout(() => this.showMessage = false, 5000);
  }

  private showErrorMessage(msg: string): void {
    this.message = msg;
    this.messageType = 'error';
    this.showMessage = true;
    setTimeout(() => this.showMessage = false, 5000);
  }
}