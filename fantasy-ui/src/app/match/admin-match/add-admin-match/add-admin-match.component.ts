import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, FormControl, AbstractControl } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { MatchService, Match } from '../../match.service'; 
import { TeamService, TeamIcon } from '../../team.service';
import { Router } from '@angular/router';
import { TEAMS } from '../../../shared/constants/team-list';
import { Observable, startWith, map } from 'rxjs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { NgxMaterialTimepickerModule } from 'ngx-material-timepicker';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { GameweekService, Gameweek } from '../../../gameweek/gameweek.service';

@Component({
  selector: 'app-admin-match',
  templateUrl: './add-admin-match.component.html',
  styleUrls: ['./add-admin-match.component.scss'],
  standalone: true,
  imports: [
    CommonModule, 
    ReactiveFormsModule, 
    FormsModule, 
    MatSnackBarModule,
    MatFormFieldModule,
    MatInputModule,
    MatAutocompleteModule,
    MatIconModule,
    MatButtonModule,
    MatDatepickerModule,
    MatNativeDateModule,
    NgxMaterialTimepickerModule,
  ],
})
export class AddAdminMatchComponent implements OnInit {
  matchForm: FormGroup;
  isSubmitting: boolean = false;
  teams = TEAMS;
  
  // Team icons properties
  teamsWithIcons: TeamIcon[] = [];
  teamIconsMap: {[key: string]: string} = {};
  gameweeks: Gameweek[] = [];
  
  // Gameweek properties
  selectedGameweeks: Gameweek[] = [];
  gameweekInput = new FormControl('');

  // Observables pour l'autocomplete
  filteredHomeTeams!: Observable<TeamIcon[]>;
  filteredAwayTeams!: Observable<TeamIcon[]>;
  filteredGameweeks!: Observable<Gameweek[]>;
  
  constructor(
    private matchService: MatchService, 
    public teamService: TeamService,
    private fb: FormBuilder, 
    private router: Router, 
    private snackBar: MatSnackBar,
    private gameweekService: GameweekService
  ) {
    this.matchForm = this.fb.group({
      homeTeam: ['', [Validators.required, this.teamValidator.bind(this)]],
      awayTeam: ['', [Validators.required, this.teamValidator.bind(this)]],
      matchDate: ['', Validators.required],
      matchTime: ['', Validators.required],
      homeScore: [0],
      awayScore: [0],
      description: [''],
      status: ['SCHEDULED', Validators.required],
      active: [true],
      gameweeks: [[]],
    }, { validators: this.teamsCannotBeSame });
  }

  ngOnInit(): void {
    this.loadTeamIcons();
    this.gameweekService.getAllGameweeks().subscribe(data => {
      this.gameweeks = data;
      this.setupGameweekAutocomplete();
    });
  }

  // Load team icons
  loadTeamIcons(): void {
    this.teamService.getAllTeamIcons().subscribe({
      next: (icons) => {
        this.teamIconsMap = icons;
        this.teamsWithIcons = this.teamService.getTeamsWithIcons(this.teams, icons)
          .filter(team => this.teams.includes(team.name));
        this.setupAutocomplete();
      },
      error: (error) => {
        console.error('Error loading team icons:', error);
        this.teamsWithIcons = this.teams.map(team => ({
          name: team,
          iconUrl: this.teamService.getDefaultIconPath(team),
          league: 'Unknown'
        }));
        this.setupAutocomplete();
      }
    });
  }

  // Setup autocomplete
  private setupAutocomplete(): void {
    const homeTeamControl = this.matchForm.get('homeTeam')!;
    const awayTeamControl = this.matchForm.get('awayTeam')!;

    this.filteredHomeTeams = homeTeamControl.valueChanges.pipe(
      startWith(''),
      map(value => this.filterTeams(value))
    );

    this.filteredAwayTeams = awayTeamControl.valueChanges.pipe(
      startWith(''),
      map(value => this.filterTeams(value))
    );
  }

  // Setup gameweek autocomplete
  private setupGameweekAutocomplete(): void {
    this.filteredGameweeks = this.gameweekInput.valueChanges.pipe(
      startWith(''),
      map(value => this.filterGameweeks(value || ''))
    );
  }

  // Filter teams based on input
  private filterTeams(value: string): TeamIcon[] {
    if (!value || typeof value !== 'string') {
      return this.teamsWithIcons;
    }
    
    const filterValue = value.toLowerCase().trim();
    return this.teamsWithIcons.filter(team => 
      team.name.toLowerCase().includes(filterValue)
    );
  }

  // Filter gameweeks based on input
  private filterGameweeks(value: string): Gameweek[] {
    if (!value || typeof value !== 'string') {
      return this.gameweeks;
    }
    
    const filterValue = value.toLowerCase().trim();
    return this.gameweeks.filter(gameweek => {
      const description = gameweek.description;
      return description ? description.toLowerCase().includes(filterValue) : false;
    });
  }

  // Add gameweek to selection
  addGameweek(gameweek: Gameweek): void {
    if (gameweek && !this.isGameweekSelected(gameweek.id!)) {
      this.selectedGameweeks.push(gameweek);
      this.updateFormGameweeks();
    }
  }

  // Remove gameweek from selection
  removeGameweek(gameweekId: number): void {
    this.selectedGameweeks = this.selectedGameweeks.filter(gw => gw.id !== gameweekId);
    this.updateFormGameweeks();
  }

  // Clear all selected gameweeks
  clearAllGameweeks(): void {
    this.selectedGameweeks = [];
    this.updateFormGameweeks();
  }

  // Check if gameweek is already selected
  isGameweekSelected(gameweekId: number): boolean {
    return this.selectedGameweeks.some(gw => gw.id === gameweekId);
  }

  // Update form control with selected gameweeks
  private updateFormGameweeks(): void {
    const gameweekIds = this.selectedGameweeks.map(gw => ({ id: gw.id }));
    this.matchForm.patchValue({ gameweeks: gameweekIds });
  }

  // Format gameweek date for display
  formatGameweekDate(dateString: string): string {
    if (!dateString) return '';
    
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString('fr-FR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
      });
    } catch (error) {
      return dateString;
    }
  }

  // Track by function for ngFor performance
  trackByGameweekId(index: number, gameweek: Gameweek): number {
    return gameweek.id!;
  }

  // Custom validator for teams
  private teamValidator(control: AbstractControl) {
    if (!control.value || control.value.trim() === '') {
      return null; // Let required validator handle empty values
    }
    
    const isValidTeam = this.teams.includes(control.value);
    return isValidTeam ? null : { invalidTeam: true };
  }

  // Validator to ensure teams are different
  private teamsCannotBeSame(group: AbstractControl) {
    if (!(group instanceof FormGroup)) return null;
    
    const home = group.get('homeTeam')?.value;
    const away = group.get('awayTeam')?.value;
    
    if (home && away && home === away) {
      return { sameTeams: true };
    }
    return null;
  }

  // Handle team selection from autocomplete
  onTeamSelected(teamName: string, isHomeTeam: boolean): void {
    if (isHomeTeam) {
      this.matchForm.patchValue({ homeTeam: teamName });
    } else {
      this.matchForm.patchValue({ awayTeam: teamName });
    }
  }

  // Get selected team icon
  getSelectedTeamIcon(teamName: string): TeamIcon | null {
    if (!teamName) return null;
    return this.teamsWithIcons.find(team => team.name === teamName) || null;
  }

  // Clear team selection
  clearHomeTeam(): void {
    this.matchForm.patchValue({ homeTeam: '' });
    this.matchForm.get('homeTeam')?.markAsUntouched();
  }

  clearAwayTeam(): void {
    this.matchForm.patchValue({ awayTeam: '' });
    this.matchForm.get('awayTeam')?.markAsUntouched();
  }

  // Handle image error
  onImageError(event: Event, teamName: string): void {
    const img = event.target as HTMLImageElement;
    img.src = this.teamService.getDefaultIconPath(teamName);
  }

  // Handle form submission
  onSubmit(): void {
    this.markFormGroupTouched(this.matchForm);
    if (this.matchForm.invalid) {
      this.showValidationErrors();
      return;
    }
    if (this.isSubmitting) return;
    this.isSubmitting = true;

    const formValue = this.matchForm.value;

    const date = formValue.matchDate;
    if (!(date instanceof Date) || isNaN(date.getTime())) {
      this.showSnackbar('Date invalide', 'error');
      this.isSubmitting = false;
      return;
    }

    const timeString = formValue.matchTime || '00:00';
    const [hours, minutes] = timeString.split(':');

    // ✅ Construire une date locale au format ISO sans 'Z' (indique que c'est local)
    const year = date.getFullYear();
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    const day = date.getDate().toString().padStart(2, '0');
    const localDateTimeString = `${year}-${month}-${day}T${hours}:${minutes}:00`;

    const match: Match = {
      ...formValue,
      matchDate: localDateTimeString // ✅ Pas de conversion UTC
    };

    delete (match as any).matchTime;

    if (match.status === 'SCHEDULED') {
      match.homeScore = 0;
      match.awayScore = 0;
    }
    match.homeScore = Number(match.homeScore) || 0;
    match.awayScore = Number(match.awayScore) || 0;

    this.createMatch(match);
  }

  // ✅ Fonction utilitaire
  private formatDateToISO(date: Date): string {
    const year = date.getFullYear();
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    const day = date.getDate().toString().padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  // Create new match
  private createMatch(match: Match): void {
    this.matchService.createMatch(match).subscribe({
      next: () => {
        this.showSnackbar('Match créé avec succès!', 'success');
        this.resetForm();
        
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

  // Reset form to initial state
  resetForm(): void {
    this.matchForm.reset({
      homeTeam: '',
      awayTeam: '',
      matchDate: '',
      matchTime: '',
      status: 'SCHEDULED',
      active: true,
      homeScore: 0,
      awayScore: 0,
      description: '',
      gameweeks: []
    });
    
    // Reset gameweek selection
    this.selectedGameweeks = [];
    this.gameweekInput.setValue('');
    
    this.isSubmitting = false;
    this.markFormGroupUntouched(this.matchForm);
  }

  // Show validation errors
  private showValidationErrors(): void {
    const errors = [];
    
    if (this.matchForm.get('homeTeam')?.hasError('required')) {
      errors.push('Équipe domicile requise');
    }
    if (this.matchForm.get('homeTeam')?.hasError('invalidTeam')) {
      errors.push('Équipe domicile non valide');
    }
    if (this.matchForm.get('awayTeam')?.hasError('required')) {
      errors.push('Équipe extérieure requise');
    }
    if (this.matchForm.get('awayTeam')?.hasError('invalidTeam')) {
      errors.push('Équipe extérieure non valide');
    }
    if (this.matchForm.hasError('sameTeams')) {
      errors.push('Les équipes doivent être différentes');
    }
    if (this.matchForm.get('matchDate')?.hasError('required')) {
      errors.push('Date du match requise');
    }
    if (this.matchForm.get('matchTime')?.hasError('required')) {
      errors.push('Heure du match requise');
    }

    const message = errors.length > 0 ? errors.join(', ') : 'Veuillez corriger les erreurs dans le formulaire.';
    this.showSnackbar(message, 'error');
    this.scrollToFirstError();
  }

  // Mark all form controls as touched
  private markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsTouched();

      if (control instanceof FormGroup) {
        this.markFormGroupTouched(control);
      }
    });
  }

  // Mark all form controls as untouched
  private markFormGroupUntouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsUntouched();

      if (control instanceof FormGroup) {
        this.markFormGroupUntouched(control);
      }
    });
  }

  // Scroll to first error field
  private scrollToFirstError(): void {
    const firstErrorElement = document.querySelector('.form-control.ng-invalid, .mat-form-field.ng-invalid');
    if (firstErrorElement) {
      firstErrorElement.scrollIntoView({ 
        behavior: 'smooth', 
        block: 'center' 
      });
      
      const input = firstErrorElement.querySelector('input');
      if (input) {
        input.focus();
      }
    }
  }

  // Show snackbar notification
  private showSnackbar(message: string, type: 'success' | 'error'): void {
    this.snackBar.open(message, 'Fermer', {
      duration: type === 'success' ? 3000 : 5000,
      panelClass: [`snackbar-${type}`],
      horizontalPosition: 'center',
      verticalPosition: 'top'
    });
  }

  // Helper methods for template
  get homeTeamValue(): string {
    return this.matchForm.get('homeTeam')?.value || '';
  }

  get awayTeamValue(): string {
    return this.matchForm.get('awayTeam')?.value || '';
  }

  get homeTeamIcon(): TeamIcon | null {
    return this.getSelectedTeamIcon(this.homeTeamValue);
  }

  get awayTeamIcon(): TeamIcon | null {
    return this.getSelectedTeamIcon(this.awayTeamValue);
  }

  // Get today's date in YYYY-MM-DD format
  getTodayDate(): string {
    const today = new Date();
    return today.toISOString().split('T')[0];
  }
}
