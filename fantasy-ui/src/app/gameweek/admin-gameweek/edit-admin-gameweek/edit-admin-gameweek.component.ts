import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { GameweekService, Gameweek } from '../../gameweek.service';

@Component({
  selector: 'app-edit-admin-gameweek',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './edit-admin-gameweek.component.html',
  styleUrl: './edit-admin-gameweek.component.scss'
})
export class EditAdminGameweekComponent implements OnInit {
  gameweekForm!: FormGroup;
  isLoading = false;
  isLoadingGameweek = true;
  isSubmitted = false;
  errorMessage = '';
  successMessage = '';
  gameweekId!: number;
  originalGameweek?: Gameweek;

  competitions = [
    { value: 'PREMIER_LEAGUE', label: 'Premier League' },
    { value: 'LA_LIGA', label: 'La Liga' },
    { value: 'SERIE_A', label: 'Serie A' },
    { value: 'BUNDESLIGA', label: 'Bundesliga' },
    { value: 'LIGUE_1', label: 'Ligue 1' },
    { value: 'CHAMPIONS_LEAGUE', label: 'Champions League' },
    { value: 'EUROPA_LEAGUE', label: 'Europa League' }
  ];

  statuses = [
    { value: 'UPCOMING', label: 'Upcoming' },
    { value: 'COMPLETED', label: 'Completed' },
    { value: 'CANCELLED', label: 'Cancelled' }
  ];

  constructor(
    private fb: FormBuilder,
    private gameweekService: GameweekService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.initializeForm();
    this.getGameweekId();
  }

  getGameweekId(): void {
    this.route.params.subscribe(params => {
      this.gameweekId = +params['id'];
      if (this.gameweekId) {
        this.loadGameweek();
      } else {
        this.errorMessage = 'Invalid gameweek ID';
        this.isLoadingGameweek = false;
      }
    });
  }

  loadGameweek(): void {
    this.isLoadingGameweek = true;
    this.errorMessage = '';

    this.gameweekService.getGameweekById(this.gameweekId).subscribe({
      next: (gameweek) => {
        this.originalGameweek = { ...gameweek };
        this.populateForm(gameweek);
        this.isLoadingGameweek = false;
      },
      error: (error) => {
        this.isLoadingGameweek = false;
        this.errorMessage = error.error?.message || 'Failed to load gameweek details.';
        console.error('Error loading gameweek:', error);
      }
    });
  }

  populateForm(gameweek: Gameweek): void {
    this.gameweekForm.patchValue({
      weekNumber: gameweek.weekNumber,
      status: gameweek.status,
      competition: gameweek.competition,
      startDate: this.formatDateForInput(new Date(gameweek.startDate)),
      endDate: this.formatDateForInput(new Date(gameweek.endDate)),
      joinDeadline: this.formatDateForInput(new Date(gameweek.joinDeadline)),
      description: gameweek.description || ''
    });
  }

  initializeForm(): void {
    this.gameweekForm = this.fb.group({
      weekNumber: ['', [Validators.required, Validators.min(1), Validators.max(50)]],
      status: ['', Validators.required],
      competition: ['', Validators.required],
      startDate: ['', Validators.required],
      endDate: ['', Validators.required],
      joinDeadline: ['', Validators.required],
      description: ['']
    }, { validators: this.dateValidator });
  }

  dateValidator(group: FormGroup): any {
    const startDate = group.get('startDate')?.value;
    const endDate = group.get('endDate')?.value;
    const joinDeadline = group.get('joinDeadline')?.value;

    if (!startDate || !endDate || !joinDeadline) {
      return null;
    }

    const start = new Date(startDate);
    const end = new Date(endDate);
    const deadline = new Date(joinDeadline);

    if (end <= start) {
      return { endDateInvalid: true };
    }

    if (deadline > start) {
      return { joinDeadlineInvalid: true };
    }

    return null;
  }

  formatDateForInput(date: Date): string {
    return date.toISOString().slice(0, 16);
  }

  get f() {
    return this.gameweekForm.controls;
  }

  onSubmit(): void {
    this.isSubmitted = true;
    this.errorMessage = '';
    this.successMessage = '';

    if (this.gameweekForm.invalid) {
      this.markFormGroupTouched();
      return;
    }

    this.isLoading = true;
    const gameweekData: Gameweek = {
      ...this.gameweekForm.value,
      id: this.gameweekId
    };

    this.gameweekService.updateGameweek(this.gameweekId, gameweekData).subscribe({
      next: (response) => {
        this.isLoading = false;
        this.successMessage = 'Gameweek updated successfully!';
        setTimeout(() => {
          this.router.navigate(['/admin/allgameweek']);
        }, 2000);
      },
      error: (error) => {
        this.isLoading = false;
        this.errorMessage = error.error?.message || 'An error occurred while updating the gameweek.';
        console.error('Error updating gameweek:', error);
      }
    });
  }

  markFormGroupTouched(): void {
    Object.keys(this.gameweekForm.controls).forEach(key => {
      this.gameweekForm.get(key)?.markAsTouched();
    });
  }

  onReset(): void {
    if (this.originalGameweek) {
      this.populateForm(this.originalGameweek);
      this.isSubmitted = false;
      this.errorMessage = '';
      this.successMessage = '';
    }
  }

  onCancel(): void {
    this.router.navigate(['/admin/allgameweek']);
  }

  getFieldError(fieldName: string): string {
    const field = this.gameweekForm.get(fieldName);
    if (field?.errors && (field.touched || this.isSubmitted)) {
      if (field.errors['required']) {
        return `${this.getFieldLabel(fieldName)} is required.`;
      }
      if (field.errors['min']) {
        return `${this.getFieldLabel(fieldName)} must be at least ${field.errors['min'].min}.`;
      }
      if (field.errors['max']) {
        return `${this.getFieldLabel(fieldName)} must be at most ${field.errors['max'].max}.`;
      }
    }
    return '';
  }

  getFormError(): string {
    if (this.gameweekForm.errors && (this.isSubmitted || this.gameweekForm.touched)) {
      if (this.gameweekForm.errors['endDateInvalid']) {
        return 'End date must be after start date.';
      }
      if (this.gameweekForm.errors['joinDeadlineInvalid']) {
        return 'Join deadline must be before or equal to start date.';
      }
    }
    return '';
  }

  private getFieldLabel(fieldName: string): string {
    const labels: { [key: string]: string } = {
      weekNumber: 'Week Number',
      status: 'Status',
      competition: 'Competition',
      startDate: 'Start Date',
      endDate: 'End Date',
      joinDeadline: 'Join Deadline',
      description: 'Description'
    };
    return labels[fieldName] || fieldName;
  }
}