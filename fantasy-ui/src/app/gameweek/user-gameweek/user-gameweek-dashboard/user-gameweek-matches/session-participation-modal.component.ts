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
  template: `
    <div class="modal-overlay" *ngIf="isVisible" (click)="onOverlayClick($event)">
      <div class="modal-content" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <h2>Rejoindre une Compétition</h2>
          <button class="close-btn" (click)="closeModal()">&times;</button>
        </div>
        
        <form [formGroup]="participationForm" (ngSubmit)="onSubmit()">
          <div class="form-group">
            <label for="sessionType">Type de Session:</label>
            <select id="sessionType" formControlName="sessionType" class="form-control">
              <option value="">Sélectionner un type</option>
              <option value="ONE_VS_ONE">1 vs 1 (2 joueurs)</option>
              <option value="SMALL_GROUP">Petit Groupe (3-10 joueurs)</option>
              <option value="MEDIUM_GROUP">Groupe Moyen (11-50 joueurs)</option>
              <option value="OPEN_ROOM">Salle Ouverte (50+ joueurs)</option>
            </select>
            <div class="error-message" *ngIf="participationForm.get('sessionType')?.invalid && participationForm.get('sessionType')?.touched">
              Veuillez sélectionner un type de session
            </div>
          </div>

          <div class="form-group">
            <label for="buyInAmount">Mise (€):</label>
            <input 
              type="number" 
              id="buyInAmount" 
              formControlName="buyInAmount" 
              class="form-control"
              min="0"
              step="0.01"
              placeholder="Ex: 5.00 (0 pour gratuit)">
            <div class="error-message" *ngIf="participationForm.get('buyInAmount')?.invalid && participationForm.get('buyInAmount')?.touched">
              Veuillez entrer un montant valide
            </div>
          </div>

          <div class="form-group">
            <label class="checkbox-container">
              <input type="checkbox" formControlName="isPrivate">
              <span class="checkmark"></span>
              Session privée
            </label>
          </div>

          <div class="form-group" *ngIf="participationForm.get('isPrivate')?.value">
            <label for="accessKey">Clé d'accès:</label>
            <input 
              type="text" 
              id="accessKey" 
              formControlName="accessKey" 
              class="form-control"
              placeholder="Entrez la clé d'accès">
            <div class="error-message" *ngIf="participationForm.get('accessKey')?.invalid && participationForm.get('accessKey')?.touched">
              La clé d'accès est requise pour les sessions privées
            </div>
          </div>

          <div class="prediction-summary" *ngIf="predictions.length > 0">
            <h4>Résumé de vos prédictions:</h4>
            <p>{{ predictions.length }} match(es) prédit(s)</p>
            <p *ngIf="tiebreakerCount > 0">{{ tiebreakerCount }} tie-break(s) inclus</p>
          </div>

          <div class="modal-footer">
            <button type="button" class="btn btn-secondary" (click)="closeModal()">
              Annuler
            </button>
            <button 
              type="submit" 
              class="btn btn-primary" 
              [disabled]="participationForm.invalid || isLoading">
              <span *ngIf="isLoading" class="spinner"></span>
              {{ isLoading ? 'Traitement...' : 'Confirmer et Rejoindre' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  `,
  styles: [`
    .modal-overlay {
      position: fixed;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background-color: rgba(0, 0, 0, 0.5);
      display: flex;
      justify-content: center;
      align-items: center;
      z-index: 1000;
    }

    .modal-content {
      background: white;
      border-radius: 8px;
      padding: 0;
      min-width: 450px;
      max-width: 500px;
      max-height: 90vh;
      overflow-y: auto;
      box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
    }

    .modal-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 20px 24px;
      border-bottom: 1px solid #e0e0e0;
    }

    .modal-header h2 {
      margin: 0;
      font-size: 1.5rem;
      font-weight: 600;
      color: #333;
    }

    .close-btn {
      background: none;
      border: none;
      font-size: 24px;
      cursor: pointer;
      color: #666;
      padding: 0;
      width: 30px;
      height: 30px;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .close-btn:hover {
      color: #333;
    }

    form {
      padding: 24px;
    }

    .form-group {
      margin-bottom: 20px;
    }

    label {
      display: block;
      margin-bottom: 6px;
      font-weight: 500;
      color: #333;
    }

    .form-control {
      width: 100%;
      padding: 10px 12px;
      border: 1px solid #ddd;
      border-radius: 4px;
      font-size: 14px;
      box-sizing: border-box;
    }

    .form-control:focus {
      outline: none;
      border-color: #007bff;
      box-shadow: 0 0 0 2px rgba(0, 123, 255, 0.25);
    }

    .checkbox-container {
      display: flex;
      align-items: center;
      cursor: pointer;
      margin-bottom: 0;
    }

    .checkbox-container input[type="checkbox"] {
      margin-right: 8px;
    }

    .error-message {
      color: #dc3545;
      font-size: 12px;
      margin-top: 4px;
    }

    .prediction-summary {
      background-color: #f8f9fa;
      padding: 16px;
      border-radius: 4px;
      margin-bottom: 20px;
    }

    .prediction-summary h4 {
      margin: 0 0 8px 0;
      font-size: 16px;
      font-weight: 500;
    }

    .prediction-summary p {
      margin: 4px 0;
      font-size: 14px;
      color: #666;
    }

    .modal-footer {
      display: flex;
      justify-content: flex-end;
      gap: 12px;
      margin-top: 24px;
      padding-top: 20px;
      border-top: 1px solid #e0e0e0;
    }

    .btn {
      padding: 10px 20px;
      border-radius: 4px;
      font-size: 14px;
      font-weight: 500;
      cursor: pointer;
      border: none;
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .btn-secondary {
      background-color: #6c757d;
      color: white;
    }

    .btn-secondary:hover {
      background-color: #5a6268;
    }

    .btn-primary {
      background-color: #007bff;
      color: white;
    }

    .btn-primary:hover:not(:disabled) {
      background-color: #0056b3;
    }

    .btn:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }

    .spinner {
      width: 16px;
      height: 16px;
      border: 2px solid #ffffff;
      border-top: 2px solid transparent;
      border-radius: 50%;
      animation: spin 1s linear infinite;
    }

    @keyframes spin {
      0% { transform: rotate(0deg); }
      100% { transform: rotate(360deg); }
    }
  `]
})
export class SessionParticipationModalComponent implements OnInit {
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