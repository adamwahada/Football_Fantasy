import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MatchService, Match } from '../../match.service'; // adapte le chemin si besoin
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';

@Component({
  selector: 'app-edit-admin-match',
  templateUrl: './edit-admin-match.component.html',
  styleUrls: ['./edit-admin-match.component.scss'],
  standalone: true,
  imports: [ReactiveFormsModule, FormsModule, CommonModule],
})
export class EditAdminMatchComponent implements OnInit {
  matchForm: FormGroup;
  matchId!: number;
  match?: Match;

  constructor(
    private route: ActivatedRoute,
    private matchService: MatchService,
    private fb: FormBuilder,
    private router: Router
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
    if (this.matchForm.invalid) {
      console.log('Form is invalid:', this.matchForm.errors);
      return;
    }

    const formValue = this.matchForm.value;

    // Combine date and time in ISO format
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

    // Si le statut est SCHEDULED, le score est forcé à 0-0
    if (match.status === 'SCHEDULED') {
      match.homeScore = 0;
      match.awayScore = 0;
    }

    this.matchService.updateMatch(this.matchId, match).subscribe({
      next: () => {
        alert('Match modifié avec succès !');
        this.router.navigate(['/admin/Allmatch']);
      },
      error: (error) => {
        console.error('Error updating match:', error);
      }
    });
  }

}