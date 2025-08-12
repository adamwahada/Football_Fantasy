// session-participation-modal.component.ts
import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { LeagueTheme, SessionType } from '../../../session-participation.service';


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

  constructor(private fb: FormBuilder) {
    this.participationForm = this.fb.group({
      sessionType: ['', Validators.required],
      buyInAmount: [0, [Validators.required, Validators.min(0)]],
      isPrivate: [false],
      accessKey: ['']
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
  }

  onSubmit() {
    if (this.participationForm.valid) {
      this.isLoading = true;

      const formValue = this.participationForm.value;
      const sessionData: SessionParticipationData = {
        gameweekId: this.gameweekId,
        competition: this.currentCompetition, // Use input, not form value
        sessionType: formValue.sessionType,
        buyInAmount: parseFloat(formValue.buyInAmount),
        isPrivate: formValue.isPrivate,
        accessKey: formValue.isPrivate ? formValue.accessKey : undefined
      };

      // Emit the data to parent component
      this.participationSubmitted.emit({
        sessionData,
        predictions: this.predictions
      });

      // Reset loading state (parent should handle closing modal)
      setTimeout(() => {
        this.isLoading = false;
      }, 1000);
    }
  }
}