import { Component, OnInit, Output, EventEmitter } from '@angular/core';
import {FormBuilder, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SupportService } from '../../service/support.service';
import { ChatService } from '../../service/chat.service';
import { SupportType, CreateSupportTicketDTO, ChatRoomDTO } from '../../models/support.models';


@Component({
  selector: 'app-support-ticket-form',

  templateUrl: './support-ticket-form.component.html',
  styleUrls: ['./support-ticket-form.component.scss'],
  standalone: true,
  imports: [
    ReactiveFormsModule
  ]
})
export class SupportTicketFormComponent implements OnInit {
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

  onTypeSelect(type: SupportType): void {
    this.selectedType = type;
    this.supportForm.patchValue({ supportType: type });
  }

  onSubmit(): void {
    if (this.supportForm.valid && !this.isLoading) {
      this.isLoading = true;

      const ticketData: CreateSupportTicketDTO = {
        supportType: this.supportForm.value.supportType,
        subject: this.supportForm.value.subject.trim(),
        description: this.supportForm.value.description.trim()
      };

      this.supportService.createSupportTicket(ticketData).subscribe({
        next: (chatRoom: ChatRoomDTO) => {
          this.isLoading = false;
          
          // Émettre l'événement pour informer le parent
          this.ticketCreated.emit(chatRoom);
          
          // Afficher un message de succès
          this.snackBar.open('Ticket de support créé avec succès !', 'Fermer', {
            duration: 3000,
            panelClass: ['success-snackbar']
          });

          // Rediriger vers le chat de support
          this.router.navigate(['/chat'], { 
            queryParams: { roomId: chatRoom.roomId, support: 'true' } 
          });
        },
        error: (error) => {
          this.isLoading = false;
          console.error('Erreur lors de la création du ticket:', error);
          
          let errorMessage = 'Erreur lors de la création du ticket de support';
          if (error.error?.message) {
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
