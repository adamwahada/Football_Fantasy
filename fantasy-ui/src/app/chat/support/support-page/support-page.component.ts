import { Component, OnInit } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SupportService } from '../../service/support.service';
import { ChatRoomDTO } from '../../models/support.models';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatSnackBarModule } from '@angular/material/snack-bar';

@Component({
  selector: 'app-support-page',
  templateUrl: './support-page.component.html',
  styleUrls: ['./support-page.component.scss'],
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatSnackBarModule]
})
export class SupportPageComponent implements OnInit {
  showTicketForm = true;
  userTickets: ChatRoomDTO[] = [];
  isLoading = false;
  isAdmin = false;
  supportForm!: FormGroup;
  supportTypes: { value: string; displayName: string }[] = [];
  selectedType: string | null = null;

  constructor(
    private supportService: SupportService,
    private router: Router,
    private route: ActivatedRoute,
    private snackBar: MatSnackBar,
    private fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.loadSupportTypes();
    this.checkUserRole();
    this.loadUserTickets();
    
    // Vérifier si on vient d'un ticket existant
    const roomId = this.route.snapshot.queryParams['roomId'];
    if (roomId) {
      this.showTicketForm = false;
    }
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

  private checkUserRole(): void {
    this.supportService.isUserAdmin().subscribe({
      next: (response) => {
        this.isAdmin = response.isAdmin;
      },
      error: (error) => {
        console.error('Erreur lors de la vérification du rôle:', error);
        this.isAdmin = false;
      }
    });
  }

  private loadUserTickets(): void {
    this.isLoading = true;
    this.supportService.getUserSupportTickets().subscribe({
      next: (tickets) => {
        this.userTickets = tickets;
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des tickets:', error);
        this.isLoading = false;
        this.snackBar.open('Erreur lors du chargement de vos tickets', 'Fermer', {
          duration: 3000,
          panelClass: ['error-snackbar']
        });
      }
    });
  }

  onTypeSelect(type: string): void {
    this.selectedType = type;
    this.supportForm.patchValue({ supportType: type });
  }

  onSubmit(): void {
    if (this.supportForm.valid && !this.isLoading) {
      this.isLoading = true;

      const ticketData = {
        supportType: this.supportForm.value.supportType,
        subject: this.supportForm.value.subject.trim(),
        description: this.supportForm.value.description.trim()
      };

      this.supportService.createSupportTicket(ticketData).subscribe({
        next: (chatRoom: ChatRoomDTO) => {
          this.isLoading = false;
          
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

  onTicketCreated(chatRoom: ChatRoomDTO): void {
    this.showTicketForm = false;
    this.loadUserTickets(); // Recharger la liste
    
    // Rediriger vers le chat de support
    this.router.navigate(['/chat'], { 
      queryParams: { roomId: chatRoom.roomId, support: 'true' } 
    });
  }

  onCreateNewTicket(): void {
    this.showTicketForm = true;
    this.router.navigate(['/support'], { queryParams: {} });
  }

  onViewTicket(ticket: ChatRoomDTO): void {
    this.router.navigate(['/chat'], { 
      queryParams: { roomId: ticket.roomId, support: 'true' } 
    });
  }

  onBackToChat(): void {
    this.router.navigate(['/chat']);
  }

  getSupportTypeIcon(type: string): string {
    return this.supportService.getSupportTypeIcon(type);
  }

  getSupportTypeDisplayName(type: string): string {
    return this.supportService.getSupportTypeDisplayName(type);
  }

  getSupportStatusDisplayName(status: string): string {
    return this.supportService.getSupportStatusDisplayName(status);
  }

  getSupportStatusColor(status: string): string {
    return this.supportService.getSupportStatusColor(status);
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

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('fr-FR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
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
