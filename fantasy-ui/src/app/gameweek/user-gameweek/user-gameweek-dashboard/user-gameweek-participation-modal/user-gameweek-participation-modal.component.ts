// user-gameweek-participation-modal.component.ts - FIXED VERSION
import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { LeagueTheme, SessionType } from '../../../session-participation.service';

// Define preconfigured amounts as decimals to match backend BigDecimal expectations
export const PRECONFIGURED_BUY_IN_AMOUNTS = [10.00, 20.00, 50.00, 100.00] as const;
export type BuyInAmount = typeof PRECONFIGURED_BUY_IN_AMOUNTS[number];

export interface SessionParticipationData {
  gameweekId: number; 
  competition: LeagueTheme;
  sessionType: SessionType;
  buyInAmount: number;
  isPrivate: boolean;
  accessKey?: string;
}

export interface PredictionPayload {
  matchId: number;
  pick: string;
  scoreHome: number | null;
  scoreAway: number | null;
}

// ✅ Enhanced error display interface
export interface ErrorDisplay {
  show: boolean;
  message: string;
  type: 'error' | 'warning' | 'info' | 'success';
  details?: any;
  canRetry?: boolean;
  suggestions?: string[];
  balanceInfo?: {
    required: string;
    current: string;
    shortage: string;
  };
}

@Component({
  selector: 'app-session-participation-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './user-gameweek-participation-modal.component.html',
  styleUrls: ['./user-gameweek-participation-modal.component.scss']
})
export class UserGameweekParticipationModalComponent implements OnInit {
  @Input() isVisible: boolean = false;
  @Input() gameweekId: number = 0;
  @Input() currentCompetition: LeagueTheme = 'PREMIER_LEAGUE';
  @Input() predictions: PredictionPayload[] = [];
  
  @Output() participationSubmitted = new EventEmitter<{
    sessionData: SessionParticipationData;
    predictions: PredictionPayload[];
  }>();
  @Output() modalClosed = new EventEmitter<void>();

  participationForm: FormGroup;
  isLoading: boolean = false;

  // ✅ Enhanced error handling properties
  errorDisplay: ErrorDisplay = {
    show: false,
    message: '',
    type: 'error',
    canRetry: false,
    suggestions: []
  };

  // Use number array with decimal values
  readonly buyInOptions = PRECONFIGURED_BUY_IN_AMOUNTS;
  selectedBuyIn: number = 10.00; // Use decimal default

  constructor(private fb: FormBuilder) {
    this.participationForm = this.fb.group({
      sessionType: ['', Validators.required],
      isPrivate: [false],
      accessKey: ['']
    });
  }

  ngOnInit() {
    console.log('[MODAL] 🎬 Modal initialized with:', {
      gameweekId: this.gameweekId,
      competition: this.currentCompetition,
      predictionsCount: this.predictions.length
    });

    // Watch for private session changes
    this.participationForm.get('isPrivate')?.valueChanges.subscribe(isPrivate => {
      const accessKeyControl = this.participationForm.get('accessKey');
      
      if (isPrivate) {
        accessKeyControl?.setValidators([Validators.required]);
      } else {
        accessKeyControl?.clearValidators();
        accessKeyControl?.setValue('');
      }
      accessKeyControl?.updateValueAndValidity();
    });
  }

  // ✅ CRITICAL FIX: Watch for isVisible changes from parent
  ngOnChanges(changes: any) {
    if (changes['isVisible']) {
      console.log('[MODAL] 🔄 isVisible changed:', {
        from: changes['isVisible'].previousValue,
        to: changes['isVisible'].currentValue,
        isFirstChange: changes['isVisible'].isFirstChange,
        stack: new Error().stack
      });
      
      // If parent wants to hide modal, clear any errors and reset state
      if (!changes['isVisible'].currentValue) {
        console.log('[MODAL] 🚪 Parent requested modal close - this should NOT happen for insufficient balance errors!');
        console.log('[MODAL] 🚪 Current error state:', this.errorDisplay);
        console.log('[MODAL] 🚪 Stack trace for modal close request:', new Error().stack);
        
        // ✅ CRITICAL FIX: Don't allow parent to close modal if there's a retryable error
        if (this.errorDisplay.show && this.errorDisplay.canRetry) {
          console.log('[MODAL] 🚫 BLOCKING parent-requested modal close - retryable error is displayed');
          console.log('[MODAL] 🚫 Error details:', this.errorDisplay);
          console.log('[MODAL] 🚫 Error message:', this.errorDisplay.message);
          console.log('[MODAL] 🚫 Error type:', this.errorDisplay.type);
          console.log('[MODAL] 🚫 Can retry:', this.errorDisplay.canRetry);
          console.log('[MODAL] 🚫 This should keep the modal open!');
          return; // Don't process the close request
        }
        
        console.log('[MODAL] 🚪 Processing modal close request - no retryable errors');
        this.clearError();
        this.isLoading = false;
      }
    }
  }

  // ✅ Add lifecycle logging
  ngOnDestroy() {
    console.log('[MODAL] 💀 Modal component destroyed');
  }

  ngAfterViewInit() {
    console.log('[MODAL] 👁️ Modal view initialized');
  }

  get tiebreakerCount(): number {
    return this.predictions.filter(p => p.scoreHome !== null || p.scoreAway !== null).length;
  }

  onOverlayClick(event: Event) {
    console.log('[MODAL] 🖱️ Overlay click detected:', {
      target: event.target,
      currentTarget: event.currentTarget,
      isLoading: this.isLoading,
      hasError: this.errorDisplay.show,
      errorCanRetry: this.errorDisplay.canRetry,
      stack: new Error().stack
    });
    
    // ✅ CRITICAL FIX: Don't allow closing modal by overlay click when there's a retryable error
    if (event.target === event.currentTarget && !this.isLoading && !this.errorDisplay.show) {
      console.log('[MODAL] 🚪 Allowing modal close via overlay click');
      this.closeModal();
    } else if (this.errorDisplay.show) {
      console.log('[MODAL] 🚫 Blocking modal close via overlay click - error is displayed');
      console.log('[MODAL] 🚫 Error details:', this.errorDisplay);
      console.log('[MODAL] 🚫 Error message:', this.errorDisplay.message);
      console.log('[MODAL] 🚫 Error type:', this.errorDisplay.type);
      console.log('[MODAL] 🚫 Can retry:', this.errorDisplay.canRetry);
    } else if (this.isLoading) {
      console.log('[MODAL] 🚫 Blocking modal close via overlay click - loading in progress');
    }
  }

  closeModal() {
    console.log('[MODAL] 🚪 Close modal requested:', {
      isLoading: this.isLoading,
      hasError: this.errorDisplay.show,
      errorCanRetry: this.errorDisplay.canRetry,
      stack: new Error().stack
    });
    
    if (this.isLoading) {
      console.log('[MODAL] ⏳ Cannot close modal while loading');
      return;
    }
    
    // ✅ CRITICAL FIX: Don't close modal if there's a retryable error
    if (this.errorDisplay.show && this.errorDisplay.canRetry) {
      console.log('[MODAL] 🚫 Blocking modal close - retryable error is displayed');
      console.log('[MODAL] 🚫 Error details:', this.errorDisplay);
      console.log('[MODAL] 🚫 Error message:', this.errorDisplay.message);
      console.log('[MODAL] 🚫 Error type:', this.errorDisplay.type);
      console.log('[MODAL] 🚫 Can retry:', this.errorDisplay.canRetry);
      console.log('[MODAL] 🚫 This should prevent the modal from closing!');
      return;
    }
    
    console.log('[MODAL] 🚪 Allowing modal close - no blocking conditions');
    this.isVisible = false;
    this.clearError();
    this.isLoading = false;
    this.modalClosed.emit();
  }

  // Method to handle buy-in selection
  selectBuyIn(amount: number) {
    this.selectedBuyIn = amount;
    this.clearError(); // Clear errors when user makes changes
    console.log('[MODAL] 💰 Selected buy-in amount:', amount, typeof amount);
  }

  // ✅ Enhanced error handling methods
  private showError(
    message: string, 
    type: 'error' | 'warning' | 'info' | 'success' = 'error', 
    details?: any,
    suggestions?: string[],
    canRetry: boolean = true,
    balanceInfo?: { required: string; current: string; shortage: string }
  ) {
    console.log('[MODAL] 🚨 showError called with:', {
      message,
      type,
      details,
      suggestions,
      canRetry,
      balanceInfo
    });
    
    this.errorDisplay = {
      show: true,
      message,
      type,
      details,
      suggestions: suggestions || [],
      canRetry,
      balanceInfo
    };
    
    console.log('[MODAL] 🚨 Error display set to:', this.errorDisplay);
    console.log('[MODAL] 🚨 Modal visibility state:', this.isVisible);
    console.log('[MODAL] 🚨 Error type:', type);
    console.log('[MODAL] 🚨 Can retry:', canRetry);
    
    // ✅ CRITICAL FIX: Ensure modal stays visible when showing errors
    if (type === 'error' && !this.isVisible) {
      console.log('[MODAL] 🚨 Modal was not visible, ensuring it stays open for error display');
      // Don't change isVisible here, let the parent manage it
    }
    
    // Auto-hide success/info messages after delay
    if (type === 'success' || type === 'info') {
      setTimeout(() => {
        this.clearError();
      }, type === 'success' ? 3000 : 5000);
    }
  }

  public clearError() {
    console.log('[MODAL] 🧹 Clearing error');
    this.errorDisplay = {
      show: false,
      message: '',
      type: 'error',
      canRetry: false,
      suggestions: []
    };
  }

  // ✅ MAIN FIX: Enhanced method to handle errors from parent component
  handleSubmissionError(error: any) {
    console.log('[MODAL] 💥 Handling submission error:', error);
    console.log('[MODAL] 💥 Current modal state before error handling:', {
      isVisible: this.isVisible,
      isLoading: this.isLoading,
      errorDisplay: this.errorDisplay
    });
    console.log('[MODAL] 💥 Stack trace for error handling:', new Error().stack);
    
    // ✅ CRITICAL FIX: Always stop loading immediately
    this.isLoading = false;

    // ✅ Define errors that should KEEP the modal OPEN for user to fix
    const KEEP_MODAL_OPEN_ERRORS = [
      'INSUFFICIENT_BALANCE',
      'ALREADY_JOINED',
      'SESSION_FULL', 
      'TERMS_NOT_ACCEPTED',
      'GAMEWEEK_NOT_FOUND',
      'VALIDATION_ERROR',
      'INVALID_BUY_IN_AMOUNT',
      'SESSION_TYPE_REQUIRED',
      'USER_ID_REQUIRED',
      'GAMEWEEK_ID_REQUIRED',
      'COMPETITION_REQUIRED',
      'PREDICTIONS_REQUIRED',
      'NETWORK_ERROR',
      'ACCESS_KEY_REQUIRED',
      // ✅ CRITICAL FIX: Add all validation-related errors
      'PREDICTIONS_MISSING',
      'PREDICTIONS_INCOMPLETE',
      'MATCH_PREDICTIONS_REQUIRED',
      'TIEBREAK_SCORES_REQUIRED'
    ];

    // ✅ CRITICAL FIX: Extract error code from various possible locations
    let errorCode = 'UNKNOWN_ERROR';
    if (error?.errorCode) {
      errorCode = error.errorCode;
    } else if (error?.error) {
      errorCode = error.error;
    } else if (typeof error === 'string') {
      errorCode = error;
    }

    const shouldKeepModalOpen = KEEP_MODAL_OPEN_ERRORS.includes(errorCode);

    console.log('[MODAL] 🔍 Error analysis:', {
      errorCode,
      shouldKeepModalOpen,
      errorObject: error,
      extractedErrorCode: errorCode,
      // ✅ CRITICAL FIX: Add more detailed error analysis
      errorKeys: Object.keys(error || {}),
      errorType: typeof error,
      errorConstructor: error?.constructor?.name
    });

    // Handle insufficient balance with detailed information
    if (errorCode === 'INSUFFICIENT_BALANCE') {
      console.log('[MODAL] 💸 Insufficient balance error details:', error);
      console.log('[MODAL] 💸 Error structure analysis:', {
        hasDetails: !!error.details,
        detailsKeys: error.details ? Object.keys(error.details) : [],
        detailsValues: error.details,
        errorKeys: Object.keys(error)
      });
      
      // ✅ CRITICAL FIX: Handle both error.details and direct properties from prediction service
      const details = error.details || {};
      let balanceMessage = 'Solde insuffisant pour rejoindre cette session';
      let suggestions = ['Veuillez recharger votre compte', 'Ou choisissez un montant inférieur'];
      let balanceInfo = undefined;
      
      // Check if we have balance info either in details or directly on the error object
      const required = details?.required || error.required;
      const current = details?.current || error.current;
      const shortage = details?.shortage || error.shortage;
      
      if (required && current) {
        balanceMessage = `Solde insuffisant. Vous avez besoin de ${required}€ mais n'avez que ${current}€`;
        
        balanceInfo = {
          required: required,
          current: current,
          shortage: shortage || this.calculateShortage(required, current)
        };
        
        if (balanceInfo.shortage && this.parseFloatSafe(balanceInfo.shortage) > 0) {
          suggestions.unshift(`Il vous manque ${balanceInfo.shortage}€`);
        }
      }
      
      // Show available lower amounts as suggestions
      const currentAmount = this.parseFloatSafe(details.current || '0');
      const availableAmounts = this.buyInOptions.filter(amount => amount <= currentAmount);
      if (availableAmounts.length > 0) {
        suggestions.push(`Montants disponibles avec votre solde: ${availableAmounts.join('€, ')}€`);
      }

      console.log('[MODAL] 💸 Showing insufficient balance error with:', {
        message: balanceMessage,
        suggestions,
        balanceInfo,
        canRetry: true
      });

      console.log('[MODAL] 💸 About to show insufficient balance error');
      
      this.showError(
        balanceMessage, 
        'error', 
        { ...details, isInsufficientBalance: true }, 
        suggestions, 
        true, // ✅ canRetry = true, keeps modal open
        balanceInfo
      );
      
      console.log('[MODAL] 💸 Error displayed, modal state after showError:', {
        isVisible: this.isVisible,
        isLoading: this.isLoading,
        errorDisplay: this.errorDisplay
      });
      
      // ✅ CRITICAL FIX: Add a small delay to ensure error is fully displayed
      setTimeout(() => {
        console.log('[MODAL] 💸 Modal state after delay:', {
          isVisible: this.isVisible,
          isLoading: this.isLoading,
          errorDisplay: this.errorDisplay
        });
      }, 100);
      
      // ✅ IMPORTANT: Return early to keep modal open
      return;
    }

    // Handle other specific errors with custom messages
    const errorMappings: Record<string, { 
      message: string; 
      type: 'error' | 'warning'; 
      suggestions?: string[];
    }> = {
      'ALREADY_JOINED': {
        message: 'Vous avez déjà rejoint cette session pour cette gameweek',
        type: 'warning',
        suggestions: ['Consultez vos participations actives', 'Choisissez une autre session']
      },
      'SESSION_FULL': {
        message: 'Cette session est complète',
        type: 'warning',
        suggestions: ['Essayez un autre montant', 'Attendez qu\'une place se libère']
      },
      'TERMS_NOT_ACCEPTED': {
        message: 'Vous devez accepter les conditions d\'utilisation',
        type: 'error',
        suggestions: ['Consultez votre profil pour accepter les conditions']
      },
      'GAMEWEEK_NOT_FOUND': {
        message: 'Gameweek non trouvée',
        type: 'error',
        suggestions: ['Actualisez la page', 'Vérifiez que la gameweek est encore disponible']
      },
      'VALIDATION_ERROR': {
        message: 'Données invalides',
        type: 'error',
        suggestions: ['Vérifiez tous les champs', 'Assurez-vous que toutes les prédictions sont complètes']
      },
      // ✅ CRITICAL FIX: Add specific handling for prediction-related errors
      'PREDICTIONS_REQUIRED': {
        message: 'Aucune prédiction trouvée. Veuillez faire vos prédictions d\'abord.',
        type: 'error',
        suggestions: ['Retournez à la page des matches', 'Faites vos prédictions 1/X/2', 'Puis revenez ici']
      },
      'PREDICTIONS_MISSING': {
        message: 'Prédictions manquantes',
        type: 'error',
        suggestions: ['Veuillez faire vos prédictions avant de continuer']
      },
      'PREDICTIONS_INCOMPLETE': {
        message: 'Prédictions incomplètes',
        type: 'error',
        suggestions: ['Veuillez compléter toutes vos prédictions']
      },
      'NETWORK_ERROR': {
        message: 'Problème de connexion réseau',
        type: 'warning',
        suggestions: ['Vérifiez votre connexion internet', 'Réessayez dans quelques instants']
      },
      'INVALID_BUY_IN_AMOUNT': {
        message: 'Montant de mise invalide',
        type: 'error',
        suggestions: ['Veuillez sélectionner un montant valide']
      },
      'SESSION_TYPE_REQUIRED': {
        message: 'Type de session requis',
        type: 'error',
        suggestions: ['Veuillez sélectionner un type de session']
      },
      'ACCESS_KEY_REQUIRED': {
        message: 'Clé d\'accès requise pour cette session privée',
        type: 'error',
        suggestions: ['Veuillez entrer la clé d\'accès fournie par l\'organisateur']
      }
    };

    const errorConfig = errorMappings[errorCode];
    if (errorConfig && shouldKeepModalOpen) {
      this.showError(
        errorConfig.message, 
        errorConfig.type, 
        error,
        errorConfig.suggestions, 
        true // ✅ canRetry = true, keeps modal open
      );
      return; // ✅ Keep modal open
    }

    // ✅ For critical errors that require navigation (authentication, etc.)
    const CLOSE_MODAL_ERRORS = [
      'USER_NOT_LOGGED_IN',
      'UNAUTHORIZED',
      'FORBIDDEN',
      'TOKEN_EXPIRED'
    ];

    if (CLOSE_MODAL_ERRORS.includes(errorCode)) {
      const message = error?.message || 'Erreur d\'authentification';
      this.showError(
        message, 
        'error', 
        error,
        ['Reconnectez-vous à votre compte'],
        false // ✅ canRetry = false, will close modal
      );
      
      // Close modal after a delay for authentication errors
      setTimeout(() => {
        this.closeModal();
      }, 3000);
      return;
    }

    // ✅ For unknown/unexpected errors - keep modal open by default for better UX
    if (shouldKeepModalOpen) {
      const message = error?.message || 'Une erreur est survenue lors de la soumission';
      this.showError(
        message, 
        'error', 
        error,
        ['Vérifiez vos données et réessayez', 'Contactez le support si le problème persiste'],
        true // ✅ Keep modal open for user to retry
      );
      return;
    }

    // ✅ Default case - show error and close modal only for truly unrecoverable errors
    const message = error?.message || 'Une erreur inattendue est survenue';
    this.showError(
      message, 
      'error', 
      error,
      ['Contactez le support si le problème persiste'],
      false
    );

    setTimeout(() => {
      this.closeModal();
    }, 4000);
  }

  // ✅ Method to handle successful submission
  handleSubmissionSuccess(message?: string) {
    console.log('[MODAL] ✅ Handling submission success:', message);
    
    this.isLoading = false;
    this.clearError();
    
    // Show success message
    this.showError(
      message || 'Prédictions soumises avec succès!', 
      'success',
      undefined,
      [],
      false
    );
    
    // Close modal after success delay
    setTimeout(() => {
      this.closeModal();
    }, 2000);
  }

  // ✅ Enhanced form validation
  private validateForm(): { isValid: boolean; errorMessage?: string } {
    if (!this.participationForm.valid) {
      const errors = [];
      
      if (this.participationForm.get('sessionType')?.hasError('required')) {
        errors.push('Veuillez sélectionner un type de session');
      }
      
      if (this.participationForm.get('isPrivate')?.value && 
          this.participationForm.get('accessKey')?.hasError('required')) {
        errors.push('Veuillez entrer une clé d\'accès pour une session privée');
      }
      
      return {
        isValid: false,
        errorMessage: errors.join('. ')
      };
    }

    if (!this.selectedBuyIn || this.selectedBuyIn <= 0) {
      return {
        isValid: false,
        errorMessage: 'Veuillez sélectionner un montant de mise valide'
      };
    }

    if (!this.predictions || this.predictions.length === 0) {
      return {
        isValid: false,
        errorMessage: 'Aucune prédiction trouvée. Veuillez faire vos prédictions d\'abord.'
      };
    }

    return { isValid: true };
  }

  // ✅ Enhanced onSubmit method
  onSubmit() {
    console.log('[MODAL] Form submission started');
    this.clearError();

    // Validate form
    const validation = this.validateForm();
    if (!validation.isValid) {
      this.showError(validation.errorMessage!, 'error', undefined, undefined, true);
      return;
    }

    this.isLoading = true;
    const formValue = this.participationForm.value;

    // Ensure clean values
    const cleanBuyInAmount = Number(this.selectedBuyIn);
    const sessionType = formValue.sessionType?.trim();

    const sessionData: SessionParticipationData = {
      gameweekId: this.gameweekId,
      competition: this.currentCompetition,
      sessionType: sessionType as SessionType,
      buyInAmount: cleanBuyInAmount,
      isPrivate: formValue.isPrivate || false,
      accessKey: (formValue.isPrivate && formValue.accessKey) ? formValue.accessKey.trim() : undefined
    };

    // Enhanced debug logs
    console.log('[MODAL SUBMIT] ✅ VALID sessionData being emitted:');
    console.log('- gameweekId:', sessionData.gameweekId, typeof sessionData.gameweekId);
    console.log('- competition:', sessionData.competition, typeof sessionData.competition);
    console.log('- sessionType:', sessionData.sessionType, typeof sessionData.sessionType);
    console.log('- buyInAmount:', sessionData.buyInAmount, typeof sessionData.buyInAmount);
    console.log('- isPrivate:', sessionData.isPrivate, typeof sessionData.isPrivate);
    console.log('- accessKey:', sessionData.accessKey);
    console.log('- Predictions count:', this.predictions.length);

    console.log('[MODAL SUBMIT] 🚀 Emitting participation data to parent...');

    // Emit the event but DO NOT close the modal here
    this.participationSubmitted.emit({
      sessionData,
      predictions: this.predictions
    });

    // ✅ IMPORTANT: The parent component MUST call either:
    // - this.modalComponent.handleSubmissionSuccess(...) to close modal on success
    // - this.modalComponent.handleSubmissionError(...) to show error and keep modal open
  }

  // ✅ Method to get error icon based on type
  getErrorIcon(): string {
    switch (this.errorDisplay.type) {
      case 'warning': return '⚠️';
      case 'info': return 'ℹ️';
      case 'success': return '✅';
      default: return '❌';
    }
  }

  // ✅ Method to get error class based on type
  getErrorClass(): string {
    return `error-alert ${this.errorDisplay.type}`;
  }

  // ✅ Helper method to calculate shortage
  private calculateShortage(required: string, current: string): string {
    try {
      const requiredAmount = this.parseFloatSafe(required);
      const currentAmount = this.parseFloatSafe(current);
      const shortage = requiredAmount - currentAmount;
      return Math.max(0, shortage).toFixed(2);
    } catch (e) {
      return '0.00';
    }
  }

  // ✅ Safe parseFloat method for use in component and template
  parseFloatSafe(value: string | number | null | undefined): number {
    if (value === null || value === undefined || value === '') {
      return 0;
    }
    const parsed = parseFloat(String(value));
    return isNaN(parsed) ? 0 : parsed;
  }

  // ✅ Method to check if current error is insufficient balance
  isInsufficientBalanceError(): boolean {
    return this.errorDisplay.details?.isInsufficientBalance === true ||
           this.errorDisplay.details?.error === 'INSUFFICIENT_BALANCE';
  }

  // ✅ Method to suggest alternative buy-in amounts based on user's balance
  getSuggestedAmounts(): number[] {
    if (!this.errorDisplay.balanceInfo?.current) {
      return [];
    }
    
    const currentBalance = parseFloat(this.errorDisplay.balanceInfo.current);
    return this.buyInOptions.filter(amount => amount <= currentBalance);
  }

  // ✅ Method to handle selecting a suggested amount
  selectSuggestedAmount(amount: number) {
    this.selectBuyIn(amount);
    this.clearError();
  }


}