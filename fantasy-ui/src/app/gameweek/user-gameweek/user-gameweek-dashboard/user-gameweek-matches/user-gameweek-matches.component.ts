// user-gameweek-matches.component.ts
import { Component, OnInit, OnDestroy, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, ParamMap, Router } from '@angular/router';
import { Subject, forkJoin, combineLatest } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { GameweekService, Gameweek } from '../../../gameweek.service';
import { MatchWithIconsDTO } from '../../../../match/match.service';
import { TeamService, TeamIcon } from '../../../../match/team.service';
import { SessionParticipationData, PredictionPayload, UserGameweekParticipationModalComponent } from '../user-gameweek-participation-modal/user-gameweek-participation-modal.component';
import { SessionParticipationService } from '../../../session-participation.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NotificationService } from '../../../../shared/notification.service';
import { PredictionService, PredictionDTO, GameweekPredictionSubmissionDTO, PredictionError } from '../../../prediction.service';
import { AuthService } from '../../../../core/services/auth.service';

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

export interface SubmitPredictionResponse {
  success: boolean;
  message?: string;
  error?: string;
  details?: {
    shortage?: string;
    userId?: string;
    required?: string;
    current?: string;
  };
  suggestions?: {
    action?: string;
    minimumRequired?: string;
  };
  path?: string;
  timestamp?: string;
}

// ✅ CRITICAL FIX: Add interface for backend error responses
export interface BackendErrorResponse {
  success: false;
  error: string;
  message: string;
  details?: {
    required?: string;
    current?: string;
    shortage?: string;
    userId?: string;
  };
  suggestions?: {
    action?: string;
    minimumRequired?: string;
  };
  path?: string;
  timestamp?: string;
}

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
  @ViewChild(UserGameweekParticipationModalComponent)
  private modalComponent!: UserGameweekParticipationModalComponent;

  // ✅ Add getter to check if modal component is available
  get isModalComponentAvailable(): boolean {
    return !!this.modalComponent;
  }

  selectedCompetition!: LeagueTheme;
  selectedWeekNumber = 0;

  private _showParticipationModal = false;
  
  get showParticipationModal(): boolean {
    return this._showParticipationModal;
  }
  
  set showParticipationModal(value: boolean) {
    console.log('[MATCHES] 🔄 Modal visibility changing:', {
      from: this._showParticipationModal,
      to: value,
      stack: new Error().stack
    });
    
    // ✅ CRITICAL FIX: Don't allow modal to be hidden if there's an error
    if (value === false && this.modalComponent?.errorDisplay?.show && this.modalComponent?.errorDisplay?.canRetry) {
      console.log('[MATCHES] 🚫 BLOCKING modal close - error is displayed and retryable!');
      console.log('[MATCHES] 🚫 Error details:', this.modalComponent.errorDisplay);
      console.log('[MATCHES] 🚫 This should prevent the modal from closing!');
      return; // Don't close the modal
    }
    
    console.log('[MATCHES] 🔄 Allowing modal visibility change to:', value);
    this._showParticipationModal = value;
  }
  preparedPredictions: PredictionPayload[] = [];
  isSubmitting = false;
  message = '';
  messageType: 'success' | 'error' = 'success';
  showMessage = false;

  matches: UIMatch[] = [];
  loading = false;
  error: string | null = null;
  teamIconsMap: Record<string, string> = {};
  teamsWithIcons: TeamIcon[] = [];

  deadline: Date | null = null;
  deadlineCountdown = '';
  private countdownInterval: any;

  private destroy$ = new Subject<void>();
  private currentGameweekId: number | null = null;

  leagueDisplayName = '';
  leagueIconUrl = '';
  leagueIcons: Record<string, string> = {};

  private leagueConfigs: Record<LeagueTheme, LeagueConfig> = {
    'PREMIER_LEAGUE': { displayName: 'Premier League', iconKey: 'Premier League', theme: 'premier-league' },
    'LA_LIGA': { displayName: 'La Liga', iconKey: 'La Liga', theme: 'la-liga' },
    'SERIE_A': { displayName: 'Serie A', iconKey: 'Serie A', theme: 'serie-a' },
    'BUNDESLIGA': { displayName: 'Bundesliga', iconKey: 'Bundesliga', theme: 'bundesliga' },
    'LIGUE_ONE': { displayName: 'Ligue 1', iconKey: 'Ligue 1', theme: 'ligue-1' },
    'CHAMPIONS_LEAGUE': { displayName: 'Champions League', iconKey: 'Champions League', theme: 'champions-league' },
    'EUROPA_LEAGUE': { displayName: 'Europa League', iconKey: 'Europa League', theme: 'europa-league' },
    'CONFERENCE_LEAGUE': { displayName: 'Conference League', iconKey: 'Conference League', theme: 'conference-league' },
    'BESTOFF': { displayName: 'Best Of', iconKey: 'Best Of', theme: 'best-of' }
  };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private gameweekService: GameweekService,
    private teamService: TeamService,
    private sessionParticipationService: SessionParticipationService,
    private snackBar: MatSnackBar,
    private notificationService: NotificationService,
    private predictionService: PredictionService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    combineLatest([
      this.teamService.getAllLeagueIcons(),
      this.teamService.getAllTeamIcons(),
      this.route.paramMap
    ])
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

  // ✅ Add lifecycle hook to check modal component availability
  ngAfterViewInit() {
    console.log('[MATCHES] 👁️ View initialized, modal component available:', this.isModalComponentAvailable);
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
          if (!gw?.id) {
            this.error = 'Gameweek not found.';
            this.loading = false;
            return;
          }
          this.currentGameweekId = gw.id;
          this.deadline = gw.joinDeadline ? new Date(gw.joinDeadline) : null;
          this.updateDeadlineCountdown();
          if (this.countdownInterval) clearInterval(this.countdownInterval);
          this.countdownInterval = setInterval(() => this.updateDeadlineCountdown(), 1000);

          forkJoin({
            tiebreakers: this.gameweekService.getTiebreakerMatches(gw.id),
            matches: this.gameweekService.getMatchesWithIcons(gw.id)
          }).pipe(takeUntil(this.destroy$)).subscribe({
            next: ({ tiebreakers, matches }) => {
              const tbIds = (tiebreakers || []).map(m => m.id!).filter(Boolean);
              const allTeams = Array.from(new Set((matches || []).flatMap(m => [m.homeTeam, m.awayTeam])));
              const teamsWithIcons = this.teamService.getTeamsWithIcons(allTeams, this.teamIconsMap);
              const iconLookup: Record<string, string> = {};
              teamsWithIcons.forEach(ti => { iconLookup[ti.name] = ti.iconUrl; });

              this.matches = (matches || [])
                .filter(m => m.active !== false)
                .map(m => ({
                  ...m,
                  pick: null,
                  scoreHome: (m as any).scoreHome ?? null,
                  scoreAway: (m as any).scoreAway ?? null,
                  isTiebreak: tbIds.includes((m as any).id),
                  homeIconUrl: iconLookup[m.homeTeam] || this.teamService.getDefaultIconPath(m.homeTeam),
                  awayIconUrl: iconLookup[m.awayTeam] || this.teamService.getDefaultIconPath(m.awayTeam)
                }));
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
    const parts: string[] = [];
    if (days > 0) parts.push(`${days} day${days > 1 ? 's' : ''}`);
    if (hours > 0 || days > 0) parts.push(`${hours} hour${hours > 1 ? 's' : ''}`);
    parts.push(`${minutes} minute${minutes > 1 ? 's' : ''}`);
    this.deadlineCountdown = parts.join(' ');
  }

  formatCompetitionName(enumName: string): string {
    const config = this.leagueConfigs[enumName as LeagueTheme];
    return config?.displayName || enumName
      .toLowerCase()
      .split('_')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  }

  getLeagueIconUrl(competition: string): string {
    const config = this.leagueConfigs[competition as LeagueTheme];
    if (!config || !this.leagueIcons[config.iconKey]) {
      return this.teamService.getLeagueIconUrl('/assets/images/leagues/default.png');
    }
    return this.teamService.getLeagueIconUrl(this.leagueIcons[config.iconKey]);
  }

  handleImageError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.src = this.teamService.getLeagueIconUrl('/assets/images/leagues/default.png');
    img.classList.add('image-error');
  }

  selectPick(match: UIMatch, pick: PickOption): void {
    match.pick = match.pick === pick ? null : pick;
  }

  resetPredictions(): void {
    this.matches.forEach(m => {
      m.pick = null;
      if (m.isTiebreak) {
        m.scoreHome = null;
        m.scoreAway = null;
      }
    });
  }

  submitPredictions(): void {
    const missingPicks = this.matches.filter(m => !m.pick);
    if (missingPicks.length) {
      this.showErrorMessage(`Veuillez choisir 1/X/2 pour tous les matches.`);
      return;
    }

    const invalidTiebreaks = this.matches.filter(m => m.isTiebreak && (m.scoreHome === null || m.scoreAway === null));
    if (invalidTiebreaks.length) {
      this.showErrorMessage('Veuillez renseigner les scores pour les tie-breaks.');
      return;
    }

    if (!this.selectedCompetition || !(this.selectedCompetition in this.leagueConfigs)) {
      this.showErrorMessage('Competition is not valid. Please reload the page.');
      return;
    }

    this.preparedPredictions = this.matches.map(m => ({
      matchId: Number((m as any).id),
      pick: m.pick || '',
      scoreHome: m.isTiebreak ? (m.scoreHome ?? null) : null,
      scoreAway: m.isTiebreak ? (m.scoreAway ?? null) : null
    }));

    this.showParticipationModal = true;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    if (this.countdownInterval) clearInterval(this.countdownInterval);
  }

  get tiebreakCount(): number {
    return this.matches.filter(m => m.isTiebreak).length;
  }

  cancelPredictions(): void {
    this.router.navigate(['../user-gameweek-list'], { relativeTo: this.route });
  }

  async onParticipationSubmitted(event: { sessionData: SessionParticipationData; predictions: PredictionPayload[] }): Promise<void> {
    const { sessionData, predictions } = event;
    try {
      const currentUserId = await this.getCurrentUserId();
      if (!currentUserId) {
        return this.modalComponent.handleSubmissionError({
          message: 'Utilisateur non identifié',
          errorCode: 'USER_NOT_LOGGED_IN'
        });
      }
  
      const gameweekIdToSend = this.currentGameweekId ?? sessionData.gameweekId;
      if (!gameweekIdToSend) {
        return this.modalComponent.handleSubmissionError({
          message: 'ID de la gameweek non trouvé',
          errorCode: 'GAMEWEEK_ID_MISSING'
        });
      }
  
      const buyInAmount = Number(sessionData.buyInAmount);
      if (isNaN(buyInAmount) || buyInAmount <= 0) {
        return this.modalComponent.handleSubmissionError({
          message: 'Montant de mise invalide',
          errorCode: 'INVALID_BUY_IN_AMOUNT'
        });
      }
  
      const predictionDTOs: PredictionDTO[] = predictions.map(p => ({
        matchId: p.matchId,
        predictedResult: this.mapPickToResult(p.pick),
        predictedHomeScore: p.scoreHome,
        predictedAwayScore: p.scoreAway
      }));
  
      const submissionDTO: GameweekPredictionSubmissionDTO = {
        userId: currentUserId,
        gameweekId: gameweekIdToSend,
        competition: sessionData.competition,
        predictions: predictionDTOs,
        sessionType: sessionData.sessionType,
        buyInAmount,
        isPrivate: sessionData.isPrivate,
        complete: true
      };
  
      this.predictionService
        .submitPredictionsAndJoinSession(
          submissionDTO,
          sessionData.sessionType,
          buyInAmount,
          sessionData.isPrivate,
          sessionData.accessKey
        )
        .subscribe({
          next: (response: SubmitPredictionResponse) => {
            console.log('[MATCHES] 📥 Response received:', response);
            
            // ✅ CRITICAL FIX: Check if this is actually an error response
            if (!response.success || response.error) {
              console.log('[MATCHES] 🚨 Error response detected in next callback');
              // 🚨 Error returned by backend
              this.modalComponent.handleSubmissionError({
                message: response.message || 'Une erreur est survenue',
                errorCode: response.error || 'UNKNOWN_ERROR',
                details: response.details || null
              });
              return; // modal stays open, user can change amount
            }
  
            // ✅ Success - only if truly successful
            console.log('[MATCHES] ✅ Success response confirmed');
            this.modalComponent.handleSubmissionSuccess(response.message || 'Prédictions soumises avec succès!');
            this.notificationService.show('Prédictions soumises avec succès!', 'success');
            setTimeout(() => this.router.navigate(['/user/user-gameweek-list']), 2000);
          },
          error: (error) => {
            console.log('[MATCHES] 🔥 Error received from prediction service:', error);
            console.log('[MATCHES] 🔥 Error type analysis:', {
              isError: error instanceof Error,
              hasErrorCode: 'errorCode' in error,
              errorKeys: Object.keys(error),
              errorConstructor: error.constructor.name,
              // ✅ CRITICAL FIX: Add more detailed error analysis
              errorStatus: (error as any)?.status,
              errorStatusText: (error as any)?.statusText,
              errorUrl: (error as any)?.url,
              errorName: (error as any)?.name
            });
            
            // ✅ CRITICAL FIX: Handle PredictionError properly
            if (error instanceof Error && 'errorCode' in error) {
              console.log('[MATCHES] 🔥 Handling as PredictionError');
              const errorData = {
                message: error.message || 'Une erreur est survenue',
                errorCode: (error as any).errorCode || 'UNKNOWN_ERROR',
                details: (error as any).details || null
              };
              console.log('[MATCHES] 🔥 Modal component available:', this.isModalComponentAvailable);
              console.log('[MATCHES] 🔥 About to call handleSubmissionError with:', errorData);
              if (this.isModalComponentAvailable) {
                this.modalComponent.handleSubmissionError(errorData);
                console.log('[MATCHES] 🔥 handleSubmissionError called successfully');
                console.log('[MATCHES] 🔥 Modal state after error handling:', {
                  showParticipationModal: this.showParticipationModal,
                  modalErrorState: this.modalComponent.errorDisplay
                });
              } else {
                console.error('[MATCHES] ❌ Modal component not available!');
              }
            } else {
              console.log('[MATCHES] 🔥 Handling as other error type');
              // Handle other types of errors
              const backend = (error as any)?.error || error;
              const errorData = {
                message: backend?.message || 'Une erreur est survenue',
                errorCode: backend?.error || 'UNKNOWN_ERROR',
                details: backend?.details || null
              };
              console.log('[MATCHES] 🔥 Modal component available:', this.isModalComponentAvailable);
              console.log('[MATCHES] 🔥 About to call handleSubmissionError with:', errorData);
              if (this.isModalComponentAvailable) {
                this.modalComponent.handleSubmissionError(errorData);
                console.log('[MATCHES] 🔥 handleSubmissionError called successfully');
                console.log('[MATCHES] 🔥 Modal state after error handling:', {
                  showParticipationModal: this.showParticipationModal,
                  modalErrorState: this.modalComponent.errorDisplay
                });
              } else {
                console.error('[MATCHES] ❌ Modal component not available!');
              }
            }
          }
        });
    } catch (error) {
      console.error('[MATCHES] 💥 Unexpected error:', error);
      this.modalComponent.handleSubmissionError({
        message: 'Une erreur inattendue est survenue.',
        errorCode: 'UNEXPECTED_ERROR'
      });
    }
  }
  


  private mapPickToResult(pick: string | null): 'HOME_WIN' | 'DRAW' | 'AWAY_WIN' {
    if (pick === 'HOME_WIN' || pick === '1') return 'HOME_WIN';
    if (pick === 'AWAY_WIN' || pick === '2') return 'AWAY_WIN';
    return 'DRAW';
  }

  private async getCurrentUserId(): Promise<number | null> {
    if (!this.authService.isLoggedIn()) return null;
    const userId = this.authService.getCurrentUserId();
    return userId || null;
  }

  onModalClosed(): void {
    console.log('[MATCHES] 🚪 Modal closed event received');
    console.log('[MATCHES] 🚪 Current modal state:', {
      showParticipationModal: this.showParticipationModal,
      modalComponentAvailable: this.isModalComponentAvailable,
      modalErrorState: this.modalComponent?.errorDisplay
    });
    console.log('[MATCHES] 🚪 Stack trace for modal close event:', new Error().stack);
    
    // ✅ CRITICAL FIX: Don't close modal if there's a retryable error
    if (this.modalComponent?.errorDisplay?.show && this.modalComponent?.errorDisplay?.canRetry) {
      console.log('[MATCHES] 🚫 BLOCKING modal close from onModalClosed - retryable error is displayed');
      console.log('[MATCHES] 🚫 Error details:', this.modalComponent.errorDisplay);
      console.log('[MATCHES] 🚫 Error message:', this.modalComponent.errorDisplay.message);
      console.log('[MATCHES] 🚫 Error type:', this.modalComponent.errorDisplay.type);
      console.log('[MATCHES] 🚫 Can retry:', this.modalComponent.errorDisplay.canRetry);
      return;
    }
    
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
