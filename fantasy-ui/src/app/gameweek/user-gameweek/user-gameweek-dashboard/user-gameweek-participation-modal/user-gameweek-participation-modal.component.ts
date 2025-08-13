// session-participation-modal.component.ts
import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { LeagueTheme, SessionType } from '../../../session-participation.service';
import { Router } from '@angular/router';

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

  // Use number array with decimal values
  readonly buyInOptions = PRECONFIGURED_BUY_IN_AMOUNTS;
  selectedBuyIn: number = 10.00; // Use decimal default

  constructor(private fb: FormBuilder,    private router: Router) {
    // 
    this.participationForm = this.fb.group({
      sessionType: ['', Validators.required],
      isPrivate: [false],
      accessKey: [''],

    });
  }

  ngOnInit() {
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

  get tiebreakerCount(): number {
    return this.predictions.filter(p => p.scoreHome !== null || p.scoreAway !== null).length;
  }

  onOverlayClick(event: Event) {
    if (event.target === event.currentTarget) {
      this.closeModal();
    }
  }

  closeModal() {
    this.isVisible = false;
    this.modalClosed.emit();
    this.router.navigate(['/user/user-gameweek-lisek']);
  }

  // Method to handle buy-in selection
  selectBuyIn(amount: number) {
    this.selectedBuyIn = amount;
    console.log('Selected buy-in amount:', amount, typeof amount);

  }

// Fixed onSubmit method for your modal component
onSubmit() {
  console.log('[MODAL] Form submission started');
  console.log('[MODAL] Form valid:', this.participationForm.valid);
  console.log('[MODAL] Selected buyIn:', this.selectedBuyIn, typeof this.selectedBuyIn);
  console.log('[MODAL] Form value:', this.participationForm.value);

  if (this.participationForm.valid && this.selectedBuyIn > 0) {
    this.isLoading = true;

    const formValue = this.participationForm.value;
    
    // Ensure clean values
    const cleanBuyInAmount = Number(this.selectedBuyIn);
    const sessionType = formValue.sessionType?.trim();
    
    // ✅ VALIDATE REQUIRED FIELDS BEFORE EMISSION
    if (!sessionType || sessionType === '') {
      console.error('[MODAL SUBMIT] SessionType is required:', sessionType);
      this.showErrorMessage('Veuillez sélectionner un type de session');
      this.isLoading = false;
      return;
    }

    if (isNaN(cleanBuyInAmount) || cleanBuyInAmount <= 0) {
      console.error('[MODAL SUBMIT] Invalid buyInAmount:', cleanBuyInAmount);
      this.showErrorMessage('Montant de mise invalide');
      this.isLoading = false;
      return;
    }

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

    // Validate predictions
    if (!this.predictions || this.predictions.length === 0) {
      console.error('[MODAL SUBMIT] No predictions provided');
      this.showErrorMessage('Aucune prédiction trouvée');
      this.isLoading = false;
      return;
    }

    console.log('[MODAL SUBMIT] 🚀 Emitting participation data...');
    this.participationSubmitted.emit({
      sessionData,
      predictions: this.predictions
    });
  } else {
    console.error('[MODAL SUBMIT] Form validation failed:');
    console.log('- Form valid:', this.participationForm.valid);
    console.log('- Selected buyIn > 0:', this.selectedBuyIn > 0);
    console.log('- Form errors:', this.participationForm.errors);
    
    // Check individual control errors
    Object.keys(this.participationForm.controls).forEach(key => {
      const control = this.participationForm.get(key);
      if (control?.errors) {
        console.log(`- ${key} errors:`, control.errors);
      }
    });
    
    this.showErrorMessage('Veuillez remplir tous les champs requis');
  }
}

private showErrorMessage(msg: string): void {
  // Add this method to show error messages in your modal
  alert(msg); // Replace with your preferred error display method
}

} 