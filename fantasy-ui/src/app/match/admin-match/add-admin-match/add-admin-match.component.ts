import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { MatchService, Match } from '../../match.service'; 
import { Router } from '@angular/router';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

@Component({
  selector: 'app-admin-match',
  templateUrl: './add-admin-match.component.html',
  styleUrls: ['./add-admin-match.component.scss'],
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, MatSnackBarModule],
})
export class AddAdminMatchComponent implements OnInit {
  matches: Match[] = [];
  matchForm: FormGroup;
  editingMatchId: number | null = null;
  isSubmitting: boolean = false;
  
  statuses = ['SCHEDULED', 'LIVE', 'COMPLETED', 'CANCELED'];
  
  // Status labels for display
  statusLabels: { [key: string]: string } = {
    'SCHEDULED': 'PROGRAMMÉ',
    'LIVE': 'EN COURS',
    'COMPLETED': 'TERMINÉ',
    'CANCELED': 'ANNULÉ'
  };

  // Filtres
  statusFilter: string = '';
  dateFilter: string = '';
  appliedStatusFilter: string = '';
  appliedDateFilter: string = '';

  constructor(
    private matchService: MatchService, 
    private fb: FormBuilder, 
    private router: Router, 
    private snackBar: MatSnackBar
  ) {
    this.matchForm = this.fb.group({
      homeTeam: ['', [Validators.required, Validators.minLength(2)]],
      awayTeam: ['', [Validators.required, Validators.minLength(2)]],
      matchDate: ['', Validators.required],
      matchTime: ['', Validators.required], 
      homeScore: [0],
      awayScore: [0],
      description: [''],
      status: ['SCHEDULED', Validators.required],
      active: [true],
      gameweeks: this.fb.array([]),
    });

    // Add real-time validation
    this.setupFormValidation();
  }

  ngOnInit(): void {
    this.loadMatches();
    this.setupStatusChangeHandler();
  }

  /**
   * Setup real-time form validation
   */
  private setupFormValidation(): void {
    // Mark fields as touched when they lose focus for immediate validation feedback
    Object.keys(this.matchForm.controls).forEach(key => {
      const control = this.matchForm.get(key);
      if (control) {
        control.valueChanges.subscribe(() => {
          if (control.invalid && control.errors) {
            // Clear error styling when user starts typing
            if (control.value && control.value.toString().trim() !== '') {
              control.markAsUntouched();
            }
          }
        });
      }
    });
  }

  /**
   * Setup status change handler to manage score fields
   */
  private setupStatusChangeHandler(): void {
    this.matchForm.get('status')?.valueChanges.subscribe(status => {
      this.onStatusChange();
    });
  }

  /**
   * Load all matches
   */
  loadMatches(): void {
    this.matchService.getAllMatches().subscribe({
      next: (data) => {
        this.matches = data;
      },
      error: (error) => {
        console.error('Erreur lors du chargement des matchs:', error);
        this.showSnackbar('Erreur lors du chargement des matchs.', 'error');
      }
    });
  }

  /**
   * Handle form submission with comprehensive validation
   */
  onSubmit(): void {
    // Mark all fields as touched to show validation errors
    this.markFormGroupTouched(this.matchForm);

    if (this.matchForm.invalid) {
      this.showSnackbar('Veuillez corriger les erreurs dans le formulaire.', 'error');
      this.scrollToFirstError();
      return;
    }

    // Prevent double submission
    if (this.isSubmitting) {
      return;
    }

    this.isSubmitting = true;

    const formValue = this.matchForm.value;
    
    // Combine date and time
    let combinedDateTime = '';
    if (formValue.matchDate && formValue.matchTime) {
      combinedDateTime = `${formValue.matchDate}T${formValue.matchTime}:00`;
    } else if (formValue.matchDate) {
      combinedDateTime = `${formValue.matchDate}T00:00:00`;
    }

    const match: Match = {
      ...formValue,
      matchDate: combinedDateTime
    };

    // Remove matchTime as it's combined with matchDate
    delete (match as any).matchTime;

    // Set scores to 0 for scheduled matches
    if (match.status === 'SCHEDULED') {
      match.homeScore = 0;
      match.awayScore = 0;
    }

    // Ensure scores are numbers
    match.homeScore = Number(match.homeScore) || 0;
    match.awayScore = Number(match.awayScore) || 0;

    if (this.editingMatchId) {
      this.updateMatch(match);
    } else {
      this.createMatch(match);
    }
  }

  /**
   * Create new match
   */
  private createMatch(match: Match): void {
    this.matchService.createMatch(match).subscribe({
      next: () => {
        this.showSnackbar('Match créé avec succès!', 'success');
        this.resetForm();
        this.loadMatches();
        
        // Navigate to all matches page after a short delay
        setTimeout(() => {
          this.router.navigate(['/admin/Allmatch']);
        }, 1500);
      },
      error: (error) => {
        console.error('Erreur lors de la création :', error);
        this.showSnackbar('Erreur lors de la création du match.', 'error');
      },
      complete: () => {
        this.isSubmitting = false;
      }
    });
  }

  /**
   * Update existing match
   */
  private updateMatch(match: Match): void {
    this.matchService.updateMatch(this.editingMatchId!, match).subscribe({
      next: () => {
        this.showSnackbar('Match mis à jour avec succès!', 'success');
        this.resetForm();
        this.loadMatches();
      },
      error: (error) => {
        console.error('Erreur lors de la mise à jour :', error);
        this.showSnackbar('Erreur lors de la mise à jour du match.', 'error');
      },
      complete: () => {
        this.isSubmitting = false;
      }
    });
  }

  /**
   * Edit a match
   */
  editMatch(match: Match): void {
    this.editingMatchId = match.id || null;
    
    const dateTime = this.extractDateAndTime(match.matchDate);
    
    this.matchForm.patchValue({
      homeTeam: match.homeTeam,
      awayTeam: match.awayTeam,
      matchDate: dateTime.date,
      matchTime: dateTime.time,
      homeScore: match.homeScore,
      awayScore: match.awayScore,
      description: match.description,
      status: match.status,
      active: match.active
    });

    // Scroll to form
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  /**
   * Delete a match
   */
  deleteMatch(id: number): void {
    if (confirm('Voulez-vous vraiment supprimer ce match ?')) {
      this.matchService.deleteMatch(id).subscribe({
        next: () => {
          this.showSnackbar('Match supprimé avec succès.', 'success');
          this.loadMatches();
        },
        error: (error) => {
          console.error('Erreur lors de la suppression :', error);
          this.showSnackbar('Erreur lors de la suppression du match.', 'error');
        }
      });
    }
  }

  /**
   * Toggle match active status
   */
  toggleActive(match: Match): void {
    const newStatus = !match.active;
    this.matchService.setMatchActiveStatus(match.id!, newStatus).subscribe({
      next: () => {
        match.active = newStatus;
        this.showSnackbar(
          `Match ${newStatus ? 'activé' : 'désactivé'} avec succès.`, 
          'success'
        );
      },
      error: (error) => {
        console.error('Erreur lors du changement de statut :', error);
        this.showSnackbar('Erreur lors du changement de statut.', 'error');
      }
    });
  }

  /**
   * Handle status change to manage score fields
   */
  onStatusChange(): void {
    const status = this.matchForm.get('status')?.value;
    
    if (status === 'SCHEDULED') {
      // Reset and disable scores for scheduled matches
      this.matchForm.patchValue({
        homeScore: 0,
        awayScore: 0
      });
    }
  }

  /**
   * Check if score fields should be disabled
   */
  isScoreDisabled(): boolean {
    return this.matchForm.get('status')?.value === 'SCHEDULED';
  }

  /**
   * Get display label for status
   */
  getStatusLabel(status: string): string {
    return this.statusLabels[status] || status;
  }

  /**
   * Extract date and time from datetime string for editing
   */
  extractDateAndTime(dateTime: any): { date: string, time: string } {
    if (!dateTime) return { date: '', time: '' };

    const d = new Date(dateTime);
    
    if (isNaN(d.getTime())) {
      return { date: '', time: '' };
    }
    
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    const hours = String(d.getHours()).padStart(2, '0');
    const minutes = String(d.getMinutes()).padStart(2, '0');

    return {
      date: `${year}-${month}-${day}`,
      time: `${hours}:${minutes}`
    };
  }

  /**
   * Reset form to initial state
   */
  resetForm(): void {
    this.matchForm.reset({
      status: 'SCHEDULED',
      active: true,
      homeScore: 0,
      awayScore: 0
    });
    
    this.editingMatchId = null;
    this.isSubmitting = false;
    
    // Clear all touched states and errors
    this.markFormGroupUntouched(this.matchForm);
  }

  /**
   * Mark all form controls as touched
   */
  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsTouched();

      if (control instanceof FormGroup) {
        this.markFormGroupTouched(control);
      }
    });
  }

  /**
   * Mark all form controls as untouched
   */
  private markFormGroupUntouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsUntouched();

      if (control instanceof FormGroup) {
        this.markFormGroupUntouched(control);
      }
    });
  }

/**
   * Scroll to first error field
   */
  private scrollToFirstError(): void {
    const firstErrorElement = document.querySelector('.form-control.invalid');
    if (firstErrorElement) {
      firstErrorElement.scrollIntoView({ 
        behavior: 'smooth', 
        block: 'center' 
      });
      (firstErrorElement as HTMLElement).focus();
    }
  }

  /**
   * Show snackbar notification
   */
  private showSnackbar(message: string, type: 'success' | 'error'): void {
    this.snackBar.open(message, 'Fermer', {
      duration: type === 'success' ? 3000 : 5000,
      panelClass: [`snackbar-${type}`],
      horizontalPosition: 'center',
      verticalPosition: 'top'
    });
  }

  /**
   * Apply filters to the matches list
   */
  applyFilters(): void {
    this.appliedStatusFilter = this.statusFilter;
    this.appliedDateFilter = this.dateFilter;
  }

  /**
   * Clear all filters
   */
  clearFilters(): void {
    this.statusFilter = '';
    this.dateFilter = '';
    this.appliedStatusFilter = '';
    this.appliedDateFilter = '';
  }

  /**
   * Get filtered matches based on applied filters
   */
  getFilteredMatches(): Match[] {
    let filteredMatches = [...this.matches];

    // Filter by status
    if (this.appliedStatusFilter) {
      filteredMatches = filteredMatches.filter(match => 
        match.status === this.appliedStatusFilter
      );
    }

    // Filter by date
    if (this.appliedDateFilter) {
      filteredMatches = filteredMatches.filter(match => {
        const matchDate = new Date(match.matchDate).toDateString();
        const filterDate = new Date(this.appliedDateFilter).toDateString();
        return matchDate === filterDate;
      });
    }

    return filteredMatches;
  }

  /**
   * Format date for display
   */
  formatDate(dateString: string): string {
    if (!dateString) return '';
    
    const date = new Date(dateString);
    if (isNaN(date.getTime())) return '';
    
    return date.toLocaleDateString('fr-FR', {
      weekday: 'short',
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  /**
   * Format time for display
   */
  formatTime(dateString: string): string {
    if (!dateString) return '';
    
    const date = new Date(dateString);
    if (isNaN(date.getTime())) return '';
    
    return date.toLocaleTimeString('fr-FR', {
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  /**
   * Get status badge class for styling
   */
  getStatusBadgeClass(status: string): string {
    const statusClasses: { [key: string]: string } = {
      'SCHEDULED': 'status-scheduled',
      'LIVE': 'status-live',
      'COMPLETED': 'status-completed',
      'CANCELED': 'status-canceled'
    };
    
    return statusClasses[status] || 'status-default';
  }

  /**
   * Check if match is editable
   */
  isMatchEditable(match: Match): boolean {
    // You can add business logic here to determine if a match can be edited
    // For example, completed matches might not be editable
    return match.status !== 'COMPLETED' || this.editingMatchId === match.id;
  }

  /**
   * Navigate to all matches page
   */
  goToAllMatches(): void {
    this.router.navigate(['/admin/Allmatch']);
  }

  /**
   * Get form control error message
   */
  getErrorMessage(controlName: string): string {
    const control = this.matchForm.get(controlName);
    
    if (control?.hasError('required')) {
      return `Le champ ${controlName} est requis.`;
    }
    
    if (control?.hasError('minlength')) {
      const minLength = control.errors?.['minlength'].requiredLength;
      return `Le champ doit contenir au moins ${minLength} caractères.`;
    }
    
    return 'Champ invalide.';
  }

  /**
   * Check if form field has error
   */
  hasError(controlName: string): boolean {
    const control = this.matchForm.get(controlName);
    return !!(control?.invalid && control?.touched);
  }

  /**
   * Duplicate match
   */
  duplicateMatch(match: Match): void {
    const duplicatedMatch = {
      ...match,
      homeTeam: `${match.homeTeam} (Copie)`,
      awayTeam: match.awayTeam,
      status: 'SCHEDULED',
      homeScore: 0,
      awayScore: 0,
      active: true
    };
    
    // Remove id to create new match
    delete duplicatedMatch.id;
    
    // Fill the form with duplicated data
    const dateTime = this.extractDateAndTime(duplicatedMatch.matchDate);
    
    this.matchForm.patchValue({
      homeTeam: duplicatedMatch.homeTeam,
      awayTeam: duplicatedMatch.awayTeam,
      matchDate: dateTime.date,
      matchTime: dateTime.time,
      homeScore: duplicatedMatch.homeScore,
      awayScore: duplicatedMatch.awayScore,
      description: duplicatedMatch.description,
      status: duplicatedMatch.status,
      active: duplicatedMatch.active
    });
    
    this.editingMatchId = null;
    
    // Scroll to form
    window.scrollTo({ top: 0, behavior: 'smooth' });
    
    this.showSnackbar('Match dupliqué. Modifiez les détails si nécessaire.', 'success');
  }
}