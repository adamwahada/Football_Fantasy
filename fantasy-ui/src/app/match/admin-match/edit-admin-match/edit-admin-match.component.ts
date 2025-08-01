import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MatchService, Match } from '../../match.service'; // adapte le chemin si besoin
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

@Component({
  selector: 'app-edit-admin-match',
  templateUrl: './edit-admin-match.component.html',
  styleUrls: ['./edit-admin-match.component.scss'],
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, MatSnackBarModule],
})
export class EditAdminMatchComponent implements OnInit {
  matchForm: FormGroup;
  matchId!: number;
  match?: Match;
  statuses = ['SCHEDULED', 'LIVE', 'COMPLETED', 'CANCELED'];
  isSubmitting = false;

  constructor(
    private route: ActivatedRoute,
    private matchService: MatchService,
    private fb: FormBuilder,
    private router: Router,
    private snackBar: MatSnackBar

  ) {
    this.matchForm = this.fb.group({
      homeTeam: ['', Validators.required],
      awayTeam: ['', Validators.required],
      matchDate: ['', Validators.required],
      matchTime: ['', Validators.required],
      homeScore: [null],
      awayScore: [null],
      description: [''],
      status: ['SCHEDULED', Validators.required],
      active: [true],
    });
  }

  ngOnInit() {
    this.matchId = +this.route.snapshot.paramMap.get('id')!;
    this.matchService.getMatchById(this.matchId).subscribe(match => {
      this.match = match;
      // Sépare la date et l'heure pour le formulaire
      const d = new Date(match.matchDate);
      this.matchForm.patchValue({
        ...match,
        matchDate: d.toISOString().slice(0, 10),
        matchTime: d.toTimeString().slice(0, 5),
      });
    });
  }
onSubmit(): void {
  this.isSubmitting = true;

  if (this.matchForm.invalid) {
    console.log('Form is invalid:', this.matchForm.errors);
    this.isSubmitting = false; // Ajoute ici aussi pour éviter blocage en cas d'erreur
    return;
  }

  const formValue = this.matchForm.value;

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

  delete (match as any).matchTime;

  if (match.status === 'SCHEDULED') {
    match.homeScore = 0;
    match.awayScore = 0;
  }

  this.matchService.updateMatch(this.matchId, match).subscribe({
    next: () => {
      alert('Match modifié avec succès !');
      this.router.navigate(['/admin/Allmatch']);
      this.isSubmitting = false; // ✅ ici
    },
    error: (error) => {
      console.error('Error updating match:', error);
      this.isSubmitting = false; // ✅ ici aussi
    }
  });
}

  getStatusLabel(status: string): string {
  switch (status) {
    case 'SCHEDULED': return 'Prévu';
    case 'LIVE': return 'En cours';
    case 'COMPLETED': return 'Terminé';
    case 'CANCELED': return 'Annulé';
    default: return status;
  }
}

isScoreDisabled(): boolean {
  return this.matchForm.get('status')?.value === 'SCHEDULED';
}

cancelEdit() {
  // Redirige vers la liste ou reset formulaire
  this.router.navigate(['/admin/Allmatch']);
}
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

}