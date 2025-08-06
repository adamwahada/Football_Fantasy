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
import { MatSelectModule } from '@angular/material/select';
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
    MatSelectModule,
    NgxMaterialTimepickerModule
  ],
})
export class AddAdminMatchComponent implements OnInit {
  matchForm: FormGroup;
  isSubmitting: boolean = false;
  teams = TEAMS;
  
  // Team icons properties
  teamsWithIcons: TeamIcon[] = [];
  teamIconsMap: {[key: string]: string} = {};

  // Observables pour l'autocomplete
  filteredHomeTeams!: Observable<TeamIcon[]>;
  filteredAwayTeams!: Observable<TeamIcon[]>;

  gameweeks: Gameweek[] = [];
  
  constructor(
    private matchService: MatchService, 
    public teamService: TeamService,
    private fb: FormBuilder, 
    private router: Router, 
    private snackBar: MatSnackBar,
    private gameweekService: GameweekService)
   {
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
    league: [''],
    weekNumber: [null, [ Validators.min(1)]],
  }, { validators: this.teamsCannotBeSame });
  }

  ngOnInit(): void {
    this.loadTeamIcons();
    this.loadGameweeks();
  }

  loadGameweeks(): void {
    this.gameweekService.getAllGameweeks().subscribe({
      next: (gws: Gameweek[]) => {
        this.gameweeks = gws;
        console.log('Gameweeks chargés:', this.gameweeks);
      },
      error: (err: any) => {
        console.error('Erreur chargement gameweeks', err);
        this.showSnackbar('Erreur lors du chargement des gameweeks', 'error');
      }
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

  // Validation de la date
  const date = formValue.matchDate;
  if (!(date instanceof Date) || isNaN(date.getTime())) {
    this.showSnackbar('Date invalide', 'error');
    this.isSubmitting = false;
    return;
  }

  // Construction de la date/heure
  const timeString = formValue.matchTime || '00:00';
  const [hours, minutes] = timeString.split(':');
  const year = date.getFullYear();
  const month = (date.getMonth() + 1).toString().padStart(2, '0');
  const day = date.getDate().toString().padStart(2, '0');
  const localDateTimeString = `${year}-${month}-${day}T${hours}:${minutes}:00`;

  // Construction de l'objet match
  const match: Match = {
    homeTeam: formValue.homeTeam,
    awayTeam: formValue.awayTeam,
    matchDate: localDateTimeString,
    homeScore: formValue.status === 'SCHEDULED' ? 0 : Number(formValue.homeScore) || 0,
    awayScore: formValue.status === 'SCHEDULED' ? 0 : Number(formValue.awayScore) || 0,
    description: formValue.description || '',
    status: formValue.status,
    active: formValue.active
  };

  // Cas 1 : avec gameweek
  if (formValue.weekNumber && formValue.league) {
    this.gameweekService.addMatchToGameweekFlexible(
      formValue.league,
      formValue.weekNumber,
      match
    ).subscribe({
      next: (createdMatch) => {
        console.log('Match créé avec gameweek flexible:', createdMatch);
        this.showSnackbar('Match créé avec succès!', 'success');
        this.resetForm();

        setTimeout(() => {
          this.router.navigate(['/admin/Allmatch']);
        }, 1500);
      },
      error: (error) => {
        this.handleError(error);
      },
      complete: () => {
        this.isSubmitting = false;
      }
    });
  } 
  // Cas 2 : sans gameweek
  else {
    this.matchService.createMatch(match).subscribe({
      next: (createdMatch) => {
        console.log('Match créé sans gameweek:', createdMatch);
        this.showSnackbar('Match créé avec succès !', 'success');
        this.resetForm();

        setTimeout(() => {
          this.router.navigate(['/admin/Allmatch']);
        }, 1500);
      },
      error: (error) => {
        this.handleError(error);
      },
      complete: () => {
        this.isSubmitting = false;
      }
    });
  }
}

// Méthode pour centraliser la gestion des erreurs
private handleError(error: any): void {
  console.error('Erreur lors de la création :', error);
  let errorMessage = 'Erreur lors de la création du match.';

  if (error.error?.message) {
    errorMessage = error.error.message;
  } else if (error.message) {
    errorMessage = error.message;
  }

  this.showSnackbar(errorMessage, 'error');
  this.isSubmitting = false;
}


  // Create new match using GameweekService.addMatchToGameweek
  private createMatchInGameweek(match: Match, gameweekId: number): void {
    this.gameweekService.addMatchToGameweek(gameweekId, match).subscribe({
      next: (createdMatch) => {
        console.log('Match créé et ajouté au gameweek:', createdMatch);
        this.showSnackbar('Match créé avec succès et ajouté au gameweek!', 'success');
        this.resetForm();
        
        setTimeout(() => {
          this.router.navigate(['/admin/Allmatch']);
        }, 1500);
      },
      error: (error) => {
        console.error('Erreur lors de la création :', error);
        let errorMessage = 'Erreur lors de la création du match.';
        
        if (error.error?.message) {
          errorMessage = error.error.message;
        } else if (error.message) {
          errorMessage = error.message;
        }
        
        this.showSnackbar(errorMessage, 'error');
        this.isSubmitting = false;
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
      league: '',
      weekNumber: null
    });
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