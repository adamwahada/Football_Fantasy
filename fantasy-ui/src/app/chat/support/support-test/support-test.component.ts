import {Component, EventEmitter, OnInit, Output} from '@angular/core';
import { SupportService } from '../../service/support.service';
import { CommonModule } from '@angular/common';
import {FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators} from "@angular/forms";
import {ChatRoomDTO, CreateSupportTicketDTO, SupportType} from "../../models/support.models";
import {ChatService} from "../../service/chat.service";
import {Router} from "@angular/router";
import {MatSnackBar} from "@angular/material/snack-bar";

@Component({
  selector: 'app-support-test',
  template: `
    <div class="support-ticket-form-container">
      <div class="form-header">
        <div class="header-icon">🆘</div>
        <h2>Créer un ticket de support</h2>
        <p>Décrivez votre problème et nous vous aiderons rapidement</p>
      </div>

      <form [formGroup]="supportForm" (ngSubmit)="onSubmit()" class="support-form">

        <!-- Sélection du type de support -->
        <div class="form-section">
          <label class="section-label">Type de problème *</label>
          <div class="type-selector">
            <div
                *ngFor="let type of supportTypes"
                class="type-option"
                [class.selected]="selectedType === type.value"
                (click)="onTypeSelect(type.value)"
            >
              <div class="type-icon">{{ getSupportTypeIcon(type.value) }}</div>
              <div class="type-info">
                <div class="type-name">{{ type.displayName }}</div>
                <div class="type-description">
                  {{ getTypeDescription(type.value) }}
                </div>
              </div>
              <div class="type-check" *ngIf="selectedType === type.value">✓</div>
            </div>
          </div>
          <div class="error-message" *ngIf="typeError">{{ typeError }}</div>
        </div>

        <!-- Sujet du ticket -->
        <div class="form-section">
          <label class="section-label" for="subject">Sujet *</label>
          <input
              id="subject"
              type="text"
              formControlName="subject"
              placeholder="Ex: Problème de paiement avec ma carte"
              class="form-input"
              [class.error]="supportForm.get('subject')?.invalid && supportForm.get('subject')?.touched"
          >
          <div class="error-message" *ngIf="subjectError">{{ subjectError }}</div>
          <div class="char-counter">
            {{ supportForm.get('subject')?.value?.length || 0 }}/100
          </div>
        </div>

        <!-- Description détaillée -->
        <div class="form-section">
          <label class="section-label" for="description">Description détaillée *</label>
          <textarea
              id="description"
              formControlName="description"
              placeholder="Décrivez votre problème en détail. Plus vous donnez d'informations, plus nous pourrons vous aider rapidement..."
              class="form-textarea"
              rows="6"
              [class.error]="supportForm.get('description')?.invalid && supportForm.get('description')?.touched"
          ></textarea>
          <div class="error-message" *ngIf="descriptionError">{{ descriptionError }}</div>
          <div class="char-counter">
            {{ supportForm.get('description')?.value?.length || 0 }}/1000
          </div>
        </div>

        <!-- Conseils d'aide -->
        <div class="help-tips" *ngIf="selectedType">
          <div class="tips-header">
            <span class="tips-icon">💡</span>
            <span>Conseils pour un ticket efficace</span>
          </div>
          <ul class="tips-list">
            <li *ngIf="selectedType === 'PAYMENT'">
              Incluez les détails de votre transaction (montant, date, méthode de paiement)
            </li>
            <li *ngIf="selectedType === 'TECHNICAL'">
              Décrivez les étapes qui mènent au problème et votre environnement (navigateur, OS)
            </li>
            <li *ngIf="selectedType === 'ACCOUNT'">
              Précisez si le problème concerne la connexion, les données personnelles ou les paramètres
            </li>
            <li *ngIf="selectedType === 'GENERAL'">
              Soyez aussi précis que possible pour que nous puissions vous orienter vers la bonne équipe
            </li>
          </ul>
        </div>

        <!-- Boutons d'action -->
        <div class="form-actions">
          <button
              type="button"
              class="btn-secondary"
              (click)="onCancel()"
              [disabled]="isLoading"
          >
            Annuler
          </button>
          <button
              type="submit"
              class="btn-primary"
              [disabled]="supportForm.invalid || isLoading"
          >
            <span *ngIf="!isLoading">Créer le ticket</span>
            <span *ngIf="isLoading" class="loading-spinner">
          <div class="spinner"></div>
          Création en cours...
        </span>
          </button>
        </div>

      </form>
    </div>
    <style>
      .support-ticket-form-container {
        max-width: 800px;
        margin: 0 auto;
        padding: 2rem;
        background: #ffffff;
        border-radius: 16px;
        box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;

        @media (max-width: 768px) {
          padding: 1rem;
          margin: 0 1rem;
        }
      }

      .form-header {
        text-align: center;
        margin-bottom: 2.5rem;

        .header-icon {
          font-size: 3rem;
          margin-bottom: 1rem;
          display: block;
        }

        h2 {
          color: #2c3e50;
          font-size: 2rem;
          font-weight: 600;
          margin: 0 0 0.5rem 0;
        }

        p {
          color: #7f8c8d;
          font-size: 1.1rem;
          margin: 0;
        }
      }

      .support-form {
        .form-section {
          margin-bottom: 2rem;

          .section-label {
            display: block;
            font-weight: 600;
            color: #2c3e50;
            margin-bottom: 0.75rem;
            font-size: 1rem;

            &::after {
              content: ' *';
              color: #e74c3c;
            }
          }
        }
      }

      // Sélecteur de type
         .type-selector {
           display: grid;
           grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
           gap: 1rem;
           margin-bottom: 0.5rem;

           @media (max-width: 768px) {
             grid-template-columns: 1fr;
           }
         }

      .type-option {
        display: flex;
        align-items: center;
        padding: 1.25rem;
        border: 2px solid #ecf0f1;
        border-radius: 12px;
        cursor: pointer;
        transition: all 0.3s ease;
        background: #fafbfc;
        position: relative;

        &:hover {
          border-color: #3498db;
          background: #f8f9ff;
          transform: translateY(-2px);
          box-shadow: 0 4px 12px rgba(52, 152, 219, 0.15);
        }

        &.selected {
          border-color: #3498db;
          background: linear-gradient(135deg, #3498db, #2980b9);
          color: white;
          transform: translateY(-2px);
          box-shadow: 0 8px 25px rgba(52, 152, 219, 0.3);

          .type-name {
            color: white;
          }

          .type-description {
            color: rgba(255, 255, 255, 0.9);
          }
        }

        .type-icon {
          font-size: 2rem;
          margin-right: 1rem;
          flex-shrink: 0;
        }

        .type-info {
          flex: 1;

          .type-name {
            font-weight: 600;
            font-size: 1.1rem;
            margin-bottom: 0.25rem;
            color: #2c3e50;
          }

          .type-description {
            font-size: 0.9rem;
            color: #7f8c8d;
            line-height: 1.4;
          }
        }

        .type-check {
          font-size: 1.5rem;
          font-weight: bold;
          color: white;
          margin-left: 0.5rem;
        }
      }

      // Champs de formulaire
         .form-input,
         .form-textarea {
           width: 100%;
           padding: 1rem;
           border: 2px solid #ecf0f1;
           border-radius: 8px;
           font-size: 1rem;
           transition: all 0.3s ease;
           background: #fafbfc;
           font-family: inherit;

           &:focus {
             outline: none;
             border-color: #3498db;
             background: white;
             box-shadow: 0 0 0 3px rgba(52, 152, 219, 0.1);
           }

           &.error {
             border-color: #e74c3c;
             background: #fdf2f2;

             &:focus {
               box-shadow: 0 0 0 3px rgba(231, 76, 60, 0.1);
             }
           }

           &::placeholder {
             color: #bdc3c7;
           }
         }

      .form-textarea {
        resize: vertical;
        min-height: 120px;
      }

      // Messages d'erreur
      .error-message {
        color: #e74c3c;
        font-size: 0.875rem;
        margin-top: 0.5rem;
        display: flex;
        align-items: center;

        &::before {
          content: '⚠️';
          margin-right: 0.5rem;
        }
      }

      // Compteur de caractères
         .char-counter {
           text-align: right;
           font-size: 0.8rem;
           color: #95a5a6;
           margin-top: 0.5rem;
         }

      // Conseils d'aide
      .help-tips {
        background: linear-gradient(135deg, #f8f9ff, #e8f4fd);
        border: 1px solid #d1ecf1;
        border-radius: 12px;
        padding: 1.5rem;
        margin: 1.5rem 0;

        .tips-header {
          display: flex;
          align-items: center;
          margin-bottom: 1rem;
          font-weight: 600;
          color: #2c3e50;

          .tips-icon {
            font-size: 1.2rem;
            margin-right: 0.5rem;
          }
        }

        .tips-list {
          margin: 0;
          padding-left: 1.5rem;
          color: #34495e;

          li {
            margin-bottom: 0.5rem;
            line-height: 1.5;

            &:last-child {
              margin-bottom: 0;
            }
          }
        }
      }

      // Boutons d'action
      .form-actions {
        display: flex;
        gap: 1rem;
        justify-content: flex-end;
        margin-top: 2rem;
        padding-top: 2rem;
        border-top: 1px solid #ecf0f1;

        @media (max-width: 768px) {
          flex-direction: column-reverse;
        }
      }

      .btn-primary,
      .btn-secondary {
        padding: 0.875rem 2rem;
        border: none;
        border-radius: 8px;
        font-size: 1rem;
        font-weight: 600;
        cursor: pointer;
        transition: all 0.3s ease;
        min-width: 140px;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 0.5rem;

        &:disabled {
          opacity: 0.6;
          cursor: not-allowed;
          transform: none !important;
        }
      }

      .btn-primary {
        background: linear-gradient(135deg, #3498db, #2980b9);
        color: white;

        &:hover:not(:disabled) {
          background: linear-gradient(135deg, #2980b9, #1f5f8b);
          transform: translateY(-2px);
          box-shadow: 0 4px 12px rgba(52, 152, 219, 0.3);
        }

        &:active:not(:disabled) {
          transform: translateY(0);
        }
      }

      .btn-secondary {
        background: #ecf0f1;
        color: #2c3e50;

        &:hover:not(:disabled) {
          background: #d5dbdb;
          transform: translateY(-2px);
          box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
        }

        &:active:not(:disabled) {
          transform: translateY(0);
        }
      }

      // Spinner de chargement
         .loading-spinner {
           display: flex;
           align-items: center;
           gap: 0.5rem;

           .spinner {
             width: 16px;
             height: 16px;
             border: 2px solid rgba(255, 255, 255, 0.3);
             border-top: 2px solid white;
             border-radius: 50%;
             animation: spin 1s linear infinite;
           }
         }

      @keyframes spin {
        0% { transform: rotate(0deg); }
        100% { transform: rotate(360deg); }
      }

      // Animations d'entrée
      .support-ticket-form-container {
        animation: slideInUp 0.6s ease-out;
      }

      @keyframes slideInUp {
        from {
          opacity: 0;
          transform: translateY(30px);
        }
        to {
          opacity: 1;
          transform: translateY(0);
        }
      }

      // Responsive design
      @media (max-width: 480px) {
        .support-ticket-form-container {
          padding: 1rem;
          margin: 0 0.5rem;
        }

        .form-header {
          h2 {
            font-size: 1.5rem;
          }

          p {
            font-size: 1rem;
          }
        }

        .type-option {
          padding: 1rem;

          .type-icon {
            font-size: 1.5rem;
            margin-right: 0.75rem;
          }

          .type-name {
            font-size: 1rem;
          }

          .type-description {
            font-size: 0.8rem;
          }
        }

        .form-input,
        .form-textarea {
          padding: 0.875rem;
          font-size: 0.95rem;
        }
      }

    </style>

  `,
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule]
})
export class SupportTestComponent implements OnInit {
  @Output() ticketCreated = new EventEmitter<ChatRoomDTO>();

  supportForm!: FormGroup;
  supportTypes: { value: string; displayName: string }[] = [];
  isLoading = false;
  selectedType: SupportType | null = null;

  constructor(
      private fb: FormBuilder,
      private supportService: SupportService,
      private chatService: ChatService,
      private router: Router,
      private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.loadSupportTypes();
  }

  private initForm(): void {
    this.supportForm = this.fb.group({
      supportType: ['', Validators.required],
      subject: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(100)]],
      description: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(1000)]]
    });
  }

  private loadSupportTypes(): void {
    this.supportService.getSupportTypes().subscribe({
      next: (types) => {
        this.supportTypes = types;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des types de support:', error);
        this.snackBar.open('Erreur lors du chargement des types de support', 'Fermer', {
          duration: 3000,
          panelClass: ['error-snackbar']
        });
      }
    });
  }
  onTypeSelect(type: string): void {
    this.selectedType = type as SupportType;
    this.supportForm.patchValue({ supportType: type as SupportType });
  }


  // Dans votre SupportTestComponent, mettez à jour la méthode onSubmit() :

  onSubmit(): void {
    if (this.supportForm.valid && !this.isLoading) {
      this.isLoading = true;

      const ticketData: CreateSupportTicketDTO = {
        supportType: this.supportForm.value.supportType,
        subject: this.supportForm.value.subject.trim(),
        description: this.supportForm.value.description.trim(),
        priority: 'MEDIUM' // Priorité par défaut
      };

      this.supportService.createSupportTicket(ticketData).subscribe({
        next: (response: any) => {
          this.isLoading = false;

          // La réponse contient maintenant { ticket, message, chatRoomId }
          const ticket = response.ticket;
          const chatRoomId = response.chatRoomId;

          console.log('Ticket créé:', ticket);
          console.log('ChatRoom ID:', chatRoomId);

          // Émettre l'événement avec les informations du ticket
          this.ticketCreated.emit({
            ...response,
            // Créer un objet ChatRoomDTO compatible pour l'événement
            roomId: chatRoomId,
            type: 'SUPPORT',
            name: `Support - ${ticket.supportType}`,
            description: ticket.subject
          });

          // Afficher un message de succès avec le numéro de ticket
          this.snackBar.open(
              `Ticket ${ticket.ticketId} créé avec succès ! Redirection vers le chat...`,
              'Fermer',
              {
                duration: 4000,
                panelClass: ['success-snackbar']
              }
          );

          // Rediriger vers le chat de support avec le chatRoomId
          setTimeout(() => {
            this.router.navigate(['/chat'], {
              queryParams: {
                roomId: chatRoomId,
                support: 'true',
                ticketId: ticket.ticketId
              }
            });
          }, 1500);
        },
        error: (error) => {
          this.isLoading = false;
          console.error('Erreur lors de la création du ticket:', error);

          let errorMessage = 'Erreur lors de la création du ticket de support';

          if (error.error?.error) {
            errorMessage = error.error.error;
          } else if (error.error?.message) {
            errorMessage = error.error.message;
          }

          this.snackBar.open(errorMessage, 'Fermer', {
            duration: 5000,
            panelClass: ['error-snackbar']
          });
        }
      });
    } else {
      this.markFormGroupTouched();

      // Afficher les erreurs de validation
      if (!this.selectedType) {
        this.snackBar.open('Veuillez sélectionner un type de support', 'Fermer', {
          duration: 3000,
          panelClass: ['warning-snackbar']
        });
      }
    }
  }

  private markFormGroupTouched(): void {
    Object.keys(this.supportForm.controls).forEach(key => {
      const control = this.supportForm.get(key);
      control?.markAsTouched();
    });
  }

  getSupportTypeIcon(type: string): string {
    return this.supportService.getSupportTypeIcon(type);
  }

  getSupportTypeDisplayName(type: string): string {
    return this.supportService.getSupportTypeDisplayName(type);
  }

  getTypeDescription(type: string): string {
    const descriptions: Record<string, string> = {
      'PAYMENT': 'Problèmes de paiement, facturation, remboursements',
      'TECHNICAL': 'Bugs, erreurs techniques, problèmes d\'interface',
      'ACCOUNT': 'Connexion, profil, paramètres du compte',
      'GENERAL': 'Questions générales, informations, autres'
    };
    return descriptions[type] || 'Type de support';
  }

  onCancel(): void {
    this.router.navigate(['/chat']);
  }

  // Getters pour les erreurs de validation
  get subjectError(): string {
    const control = this.supportForm.get('subject');
    if (control?.errors && control.touched) {
      if (control.errors['required']) return 'Le sujet est requis';
      if (control.errors['minlength']) return 'Le sujet doit contenir au moins 5 caractères';
      if (control.errors['maxlength']) return 'Le sujet ne peut pas dépasser 100 caractères';
    }
    return '';
  }

  get descriptionError(): string {
    const control = this.supportForm.get('description');
    if (control?.errors && control.touched) {
      if (control.errors['required']) return 'La description est requise';
      if (control.errors['minlength']) return 'La description doit contenir au moins 10 caractères';
      if (control.errors['maxlength']) return 'La description ne peut pas dépasser 1000 caractères';
    }
    return '';
  }

  get typeError(): string {
    const control = this.supportForm.get('supportType');
    if (control?.errors && control.touched) {
      if (control.errors['required']) return 'Veuillez sélectionner un type de support';
    }
    return '';
  }
}
