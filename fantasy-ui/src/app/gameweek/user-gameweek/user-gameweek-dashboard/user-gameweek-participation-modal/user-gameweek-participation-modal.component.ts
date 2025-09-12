// user-gameweek-participation-modal.component.ts - Enhanced with Balance Filtering
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
  privateMode?: 'CREATE' | 'JOIN';
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
  
  // ✅ NEW: Add user balance input to receive from parent
  @Input() userBalance: number = 0;

  // Remove createdAccessKey (backend-generated) in favor of client-provided key
  // @Input() createdAccessKey: string = '';

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

  // ✅ NEW: Private session mode management
  // 'CREATE' → backend generates and returns a key; 'JOIN' → enter an existing key
  readonly privateModes = ['CREATE', 'JOIN'] as const;

  // ✅ Bring back client-generated key for CREATE mode
  generatedAccessKey: string = '';

  constructor(private fb: FormBuilder) {
    this.participationForm = this.fb.group({
      sessionType: ['', Validators.required],
      isPrivate: [false],
      privateMode: ['CREATE'],
      accessKey: ['']
    });
  }

ngOnInit() {
  console.log('[MODAL] 🎬 Modal initialized with:', {
    gameweekId: this.gameweekId,
    competition: this.currentCompetition,
    predictionsCount: this.predictions.length,
    userBalance: this.userBalance,
    userBalanceType: typeof this.userBalance
  });

  // ✅ CRITICAL FIX: Ensure userBalance is a number
  this.userBalance = Number(this.userBalance) || 0;
  console.log('[MODAL] 💰 Converted userBalance:', this.userBalance);

  // Set initial buy-in to highest affordable amount
  this.setInitialBuyInAmount();

  // Watch for private session changes
  this.participationForm.get('isPrivate')?.valueChanges.subscribe(isPrivate => {
    this.configurePrivateValidators(isPrivate, this.participationForm.get('privateMode')?.value);
  });

  // Watch for private mode changes
  this.participationForm.get('privateMode')?.valueChanges.subscribe(mode => {
    const isPrivate = this.participationForm.get('isPrivate')?.value === true;
    this.configurePrivateValidators(isPrivate, mode);
  });

  // Initial configuration for private validators
  this.configurePrivateValidators(this.participationForm.get('isPrivate')?.value, this.participationForm.get('privateMode')?.value);
}

  // ✅ Configure validators based on private mode
  private configurePrivateValidators(isPrivate: boolean, mode: 'CREATE' | 'JOIN') {
    const accessKeyControl = this.participationForm.get('accessKey');

    if (isPrivate && mode === 'JOIN') {
      accessKeyControl?.setValidators([Validators.required]);
    } else {
      accessKeyControl?.clearValidators();
      accessKeyControl?.setValue('');
    }
    accessKeyControl?.updateValueAndValidity();

    // Generate a key for CREATE mode for user preview and submission
    if (isPrivate && mode === 'CREATE') {
      if (!this.generatedAccessKey) {
        this.generatedAccessKey = this.generateAccessKey();
      }
    } else {
      this.generatedAccessKey = '';
    }
  }

  // ✅ Generate 8-char key similar to backend default
  private generateAccessKey(): string {
    const alphabet = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
    let key = '';
    for (let i = 0; i < 8; i++) {
      key += alphabet.charAt(Math.floor(Math.random() * alphabet.length));
    }
    return key;
  }

  // ✅ Public wrapper to regenerate key for template usage
  regenerateAccessKey(): void {
    this.generatedAccessKey = this.generateAccessKey();
  }

  // ✅ Select the generated key input content (for UX)
  selectGeneratedKey(inputEl: HTMLInputElement): void {
    try {
      inputEl.select();
    } catch {}
  }

  // ✅ Copy client-generated key
  copyGeneratedKeyToClipboard(): void {
    if (!this.generatedAccessKey) return;
    navigator.clipboard?.writeText(this.generatedAccessKey)
      .then(() => {
        this.showError('Clé copiée dans le presse-papiers', 'info', undefined, [], false);
        setTimeout(() => this.clearError(), 1200);
      })
      .catch(() => {
        this.showError('Impossible de copier la clé. Copiez-la manuellement.', 'warning', undefined, [], false);
        setTimeout(() => this.clearError(), 2000);
      });
  }

  // ✅ Method to set initial buy-in to highest affordable amount
  private setInitialBuyInAmount() {
    const affordableAmounts = this.getAffordableAmounts();
    if (affordableAmounts.length > 0) {
      // Set to highest affordable amount, or keep current if it's affordable
      const currentlyAffordable = affordableAmounts.includes(this.selectedBuyIn);
      if (!currentlyAffordable) {
        this.selectedBuyIn = Math.max(...affordableAmounts);
        console.log('[MODAL] 💰 Set initial buy-in to highest affordable amount:', this.selectedBuyIn);
      }
    } else {
      // No affordable amounts - keep current selection but user will see they're all disabled
      console.log('[MODAL] 💸 No affordable amounts available. Current balance:', this.userBalance);
    }
  }

  // ✅ CRITICAL FIX: Watch for input changes including userBalance
ngOnChanges(changes: any) {
  console.log('[MODAL] 🔄 ngOnChanges called with:', changes);
  
  if (changes['userBalance']) {
    const oldBalance = changes['userBalance'].previousValue;
    const newBalance = changes['userBalance'].currentValue;
    
    console.log('[MODAL] 💰 Balance change detected:', {
      from: oldBalance,
      to: newBalance,
      type: typeof newBalance
    });
    
    // ✅ CRITICAL: Ensure it's a number
    this.userBalance = Number(newBalance) || 0;
    
    console.log('[MODAL] 💰 Processed balance:', this.userBalance);
    
    // Update initial buy-in when balance changes
    this.setInitialBuyInAmount();
    
    // Clear insufficient balance errors if balance increased
    if (this.errorDisplay.show && this.errorDisplay.details?.isInsufficientBalance) {
      if (this.userBalance >= this.selectedBuyIn) {
        console.log('[MODAL] 💰 Balance increased enough to cover selected amount - clearing error');
        this.clearError();
      }
    }
  }

  if (changes['isVisible']) {
    console.log('[MODAL] 🔄 isVisible changed:', {
      from: changes['isVisible'].previousValue,
      to: changes['isVisible'].currentValue,
      currentBalance: this.userBalance
    });
    
    if (!changes['isVisible'].currentValue) {
      if (this.errorDisplay.show) {
        console.log('[MODAL] 🚫 Blocking parent-requested modal close - error is displayed');
        return;
      }
      this.clearError();
      this.isLoading = false;
    }
  }
}

  // ✅ Method to get amounts user can afford
getAffordableAmounts(): number[] {
  const affordable = this.buyInOptions.filter(amount => {
    const canAfford = amount <= this.userBalance;
    return canAfford;
  });
  
  return affordable;
}

isAmountAffordable(amount: number): boolean {
  const affordable = amount <= this.userBalance;
  return affordable;
}


  // ✅ NEW: Method to get the CSS class for buy-in buttons
  getBuyInButtonClass(amount: number): string {
    const baseClass = 'buy-in-option';
    const isSelected = this.selectedBuyIn === amount;
    const isAffordable = this.isAmountAffordable(amount);
    
    if (!isAffordable) {
      return `${baseClass} disabled unaffordable`;
    } else if (isSelected) {
      return `${baseClass} selected`;
    } else {
      return baseClass;
    }
  }

  // ✅ NEW: Enhanced method to handle buy-in click with affordability check
  onBuyInClick(amount: number) {
    if (!this.isAmountAffordable(amount)) {
      console.log('[MODAL] 💸 User clicked unaffordable amount:', amount);
      
      // Show a helpful message about insufficient balance
      this.showError(
        `Solde insuffisant pour ${amount}€. Votre solde actuel: ${this.userBalance.toFixed(2)}€`,
        'warning',
        { isInsufficientBalance: true, clickedAmount: amount },
        [
          `Choisissez un montant inférieur ou égal à ${this.userBalance.toFixed(2)}€`,
          'Rechargez votre compte pour accéder à tous les montants'
        ],
        true
      );
      return;
    }
    
    // Select the affordable amount
    this.selectBuyIn(amount);
  }

  // ✅ ENHANCED: Method to handle buy-in selection with affordability validation
  selectBuyIn(amount: number) {
    // Double-check affordability
    if (!this.isAmountAffordable(amount)) {
      console.log('[MODAL] 💸 Cannot select unaffordable amount:', amount);
      return;
    }
    
    this.selectedBuyIn = amount;
    
    // ✅ CRITICAL FIX: Only clear errors if user is making a change that could fix the error
    if (this.errorDisplay.show && this.errorDisplay.details?.isInsufficientBalance) {
      this.clearError();
    }
    
  }

  // ✅ NEW: Method to get balance status message
getBalanceStatusMessage(): string | null {
  
  if (this.userBalance <= 0) {
    return 'Aucun solde disponible. Veuillez recharger votre compte.';
  }
  
  const affordableAmounts = this.getAffordableAmounts();
  if (affordableAmounts.length === 0) {
    return `Solde insuffisant pour toutes les options. Solde actuel: ${this.userBalance.toFixed(2)}€`;
  }
  
  if (affordableAmounts.length < this.buyInOptions.length) {
    const maxAffordable = Math.max(...affordableAmounts);
    return `Montant maximum disponible: ${maxAffordable.toFixed(2)}€`;
  }
  
  return null; // User can afford all amounts
}

  // ✅ NEW: Method to check if any amount is affordable
  hasAnyAffordableAmount(): boolean {
    return this.getAffordableAmounts().length > 0;
  }

  get tiebreakerCount(): number {
    return this.predictions.filter(p => p.scoreHome !== null || p.scoreAway !== null).length;
  }

  onOverlayClick(event: Event) {
    
    // ✅ CRITICAL FIX: Don't allow closing modal by overlay click when there's a retryable error
    if (event.target === event.currentTarget && !this.isLoading && !this.errorDisplay.show) {
      this.closeModal();
    }
  }

  closeModal() {
    
    if (this.isLoading) {
      return;
    }
    
    // ✅ CRITICAL FIX: Don't close modal if there's any error displayed
    if (this.errorDisplay.show) {
      return;
    }
    
    this.clearError();
    this.isLoading = false;
    this.modalClosed.emit();
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
    this.errorDisplay = {
      show: true,
      message,
      type,
      details,
      suggestions: suggestions || [],
      canRetry,
      balanceInfo
    };
    
    // Auto-hide success/info messages after delay
    if (type === 'success' || type === 'info') {
      setTimeout(() => {
        this.clearError();
      }, type === 'success' ? 3000 : 5000);
    }
  }

  public clearError() {
    this.errorDisplay = {
      show: false,
      message: '',
      type: 'error',
      canRetry: false,
      suggestions: []
    };
  }

  // ✅ ENHANCED: Updated to work with userBalance input
  handleSubmissionError(error: any) {
    
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
      'PREDICTIONS_MISSING',
      'PREDICTIONS_INCOMPLETE',
      'MATCH_PREDICTIONS_REQUIRED',
      'TIEBREAK_SCORES_REQUIRED'
    ];

    let errorCode = 'UNKNOWN_ERROR';
    if (error?.errorCode) {
      errorCode = error.errorCode;
    } else if (error?.error) {
      errorCode = error.error;
    } else if (typeof error === 'string') {
      errorCode = error;
    }

    const shouldKeepModalOpen = KEEP_MODAL_OPEN_ERRORS.includes(errorCode);

    // Handle insufficient balance with detailed information
    if (errorCode === 'INSUFFICIENT_BALANCE') {
      const details = error.details || {};
      let balanceMessage = 'Solde insuffisant pour rejoindre cette session';
      let suggestions = ['Veuillez recharger votre compte', 'Ou choisissez un montant inférieur'];
      let balanceInfo = undefined;
      
      // ✅ ENHANCED: Use userBalance input as fallback
      const required = details?.required || this.selectedBuyIn.toString();
      const current = details?.current || this.userBalance.toString();
      const shortage = details?.shortage || this.calculateShortage(required, current);
      
      balanceMessage = `Solde insuffisant. Vous avez besoin de ${required}€ mais n'avez que ${current}€`;
      
      balanceInfo = {
        required: required,
        current: current,
        shortage: shortage
      };
      
      if (balanceInfo.shortage && this.parseFloatSafe(balanceInfo.shortage) > 0) {
        suggestions.unshift(`Il vous manque ${balanceInfo.shortage}€`);
      }
      
      // ✅ ENHANCED: Show available amounts based on current balance
      const affordableAmounts = this.getAffordableAmounts();
      if (affordableAmounts.length > 0) {
        suggestions.push(`Montants disponibles: ${affordableAmounts.join('€, ')}€`);
      } else {
        suggestions.push('Aucun montant disponible avec votre solde actuel');
      }

      this.showError(
        balanceMessage, 
        'error', 
        { ...details, isInsufficientBalance: true }, 
        suggestions, 
        true,
        balanceInfo
      );
      
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
        true
      );
      return;
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
        false
      );
      
      setTimeout(() => {
        if (!this.errorDisplay.show || this.errorDisplay.type === 'error') {
          this.closeModal();
        }
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
        true
      );
      return;
    }

    // ✅ Default case - show error and keep modal open for better UX
    const message = error?.message || 'Une erreur inattendue est survenue';
    this.showError(
      message, 
      'error', 
      error,
      ['Vérifiez vos données et réessayez', 'Contactez le support si le problème persiste'],
      true
    );
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
    
    setTimeout(() => {
      if (!this.errorDisplay.show || this.errorDisplay.type === 'success') {
        this.closeModal();
      }
    }, 2000);
  }

  // ✅ Enhanced form validation
  private validateForm(): { isValid: boolean; errorMessage?: string } {
    if (!this.participationForm.valid) {
      const errors: string[] = [];
      
      if (this.participationForm.get('sessionType')?.hasError('required')) {
        errors.push('Veuillez sélectionner un type de session');
      }
      
      if (this.participationForm.get('isPrivate')?.value && 
          this.participationForm.get('privateMode')?.value === 'JOIN' &&
          this.participationForm.get('accessKey')?.hasError('required')) {
        errors.push('Veuillez entrer une clé d\'accès pour rejoindre une session privée');
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

    // ✅ Validate selected amount is affordable
    if (!this.isAmountAffordable(this.selectedBuyIn)) {
      return {
        isValid: false,
        errorMessage: `Solde insuffisant pour ${this.selectedBuyIn}€. Solde disponible: ${this.userBalance.toFixed(2)}€`
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

    // Resolve access key depending on mode
    let resolvedAccessKey: string | undefined = undefined;
    if (formValue.isPrivate === true) {
      if (formValue.privateMode === 'CREATE') {
        // Send the key generated on the client; backend will persist it
        resolvedAccessKey = this.generatedAccessKey || this.generateAccessKey();
      } else if (formValue.privateMode === 'JOIN') {
        const key = (formValue.accessKey || '').trim();
        resolvedAccessKey = key.length > 0 ? key : undefined;
      }
    }

    // Extra guard: JOIN must have a key
    if (formValue.isPrivate === true && formValue.privateMode === 'JOIN' && !resolvedAccessKey) {
      this.isLoading = false;
      this.showError('Clé d\'accès requise pour rejoindre une session privée', 'error', undefined, undefined, true);
      return;
    }

    const sessionData: SessionParticipationData = {
      gameweekId: this.gameweekId,
      competition: this.currentCompetition,
      sessionType: sessionType as SessionType,
      buyInAmount: cleanBuyInAmount,
      isPrivate: formValue.isPrivate || false,
      accessKey: resolvedAccessKey,
      privateMode: formValue.privateMode
    };

    console.log('[MODAL SUBMIT] 🚀 Emitting participation data to parent...', {
      isPrivate: sessionData.isPrivate,
      privateMode: formValue.privateMode,
      accessKeyIncluded: Boolean(sessionData.accessKey)
    });

    // Emit the event but DO NOT close the modal here
    this.participationSubmitted.emit({
      sessionData,
      predictions: this.predictions
    });
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

  // ✅ ENHANCED: Method to suggest alternative buy-in amounts based on user's balance
  getSuggestedAmounts(): number[] {
    return this.getAffordableAmounts();
  }

  // ✅ Method to handle selecting a suggested amount
  selectSuggestedAmount(amount: number) {
    this.selectBuyIn(amount);
    this.clearError();
  }

  // ✅ Method to check if modal should stay open
  shouldModalStayOpen(): boolean {
    if (!this.isVisible) {
      return false;
    }
    
    const hasError = this.errorDisplay.show;
    const isLoading = this.isLoading;
    
    return hasError || isLoading;
  }

  // ✅ Force modal open from parent
  forceModalOpen(): void {
    console.log('[MODAL] 🚀 Force modal open called');
    this.isVisible = true;
  }

  // ✅ Helper method to log modal state for debugging
  private logModalState(methodName: string): void {
    console.log('[MODAL] 🔄 Current Modal State:', {
      isVisible: this.isVisible,
      isLoading: this.isLoading,
      errorDisplay: this.errorDisplay,
      userBalance: this.userBalance,
      selectedBuyIn: this.selectedBuyIn,
      affordableAmounts: this.getAffordableAmounts(),
      methodCalled: methodName
    });
  }
}