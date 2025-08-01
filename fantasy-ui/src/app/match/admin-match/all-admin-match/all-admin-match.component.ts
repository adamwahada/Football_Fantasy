import { Component, OnInit } from '@angular/core';
import { MatchService,Match } from '../../match.service';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Router } from '@angular/router';

@Component({
  selector: 'app-admin-match',
  templateUrl: './all-admin-match.component.html',
  styleUrls: ['./all-admin-match.component.scss'],
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule,RouterLink],
})
export class AllAdminMatchComponent implements OnInit {
  matches: Match[] = [];
  matchForm: FormGroup;
  editingMatchId: number | null = null;
  statuses = ['SCHEDULED', 'LIVE', 'COMPLETED', 'CANCELED'];
  selectedMatchIds = new Set<number>();

  // Filtres
  statusFilter: string = '';
  dateFilter: string = '';
  appliedStatusFilter: string = '';
  appliedDateFilter: string = '';

  constructor(private matchService: MatchService,
     private fb: FormBuilder,
     private router: Router) {
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
      gameweeks: this.fb.array([]),
    });
  }

  ngOnInit(): void {
    this.loadMatches();
  }

  loadMatches(): void {
    this.matchService.getAllMatches().subscribe((data) => (this.matches = data));
  }

  onSubmit(): void {
    if (this.matchForm.invalid) {
      console.log('Form is invalid:', this.matchForm.errors);
      return;
    }

    const formValue = this.matchForm.value;
    console.log('Form value:', formValue);
    
    // Combine date and time in ISO format
    let combinedDateTime = '';
    if (formValue.matchDate && formValue.matchTime) {
      combinedDateTime = `${formValue.matchDate}T${formValue.matchTime}:00`;
    } else if (formValue.matchDate) {
      // If no time specified, use 00:00
      combinedDateTime = `${formValue.matchDate}T00:00:00`;
    }

    const match: Match = {
      ...formValue,
      matchDate: combinedDateTime // Replace separate fields with combined datetime
    };

    // Remove the matchTime field as we don't need it anymore
    delete (match as any).matchTime;

    // If status is SCHEDULED, force score to 0-0
    if (match.status === 'SCHEDULED') {
      match.homeScore = 0;
      match.awayScore = 0;
    }

    console.log('Match to be sent:', match);

    if (this.editingMatchId) {
      this.matchService.updateMatch(this.editingMatchId, match).subscribe({
        next: () => {
          this.loadMatches();
          this.resetForm();
          console.log('Match updated successfully');
        },
        error: (error) => {
          console.error('Error updating match:', error);
        }
      });
    } else {
      this.matchService.createMatch(match).subscribe({
        next: () => {
          this.loadMatches();
          this.resetForm();
          console.log('Match created successfully');
        },
        error: (error) => {
          console.error('Error creating match:', error);
        }
      });
    }
  }

  editMatch(match: Match): void {
    this.editingMatchId = match.id!;
    
    // Extract date and time from the stored datetime
    const dateTime = this.extractDateAndTime(match.matchDate);
    
    const patch = {
      ...match,
      matchDate: dateTime.date,
      matchTime: dateTime.time
    };
    
    this.matchForm.patchValue(patch);
  }

  deleteMatch(id: number): void {
    if (confirm('Voulez-vous supprimer ce match ?')) {
      this.matchService.deleteMatch(id).subscribe(() => this.loadMatches());
    }
  }

  toggleActive(match: Match): void {
    const newStatus = !match.active;
    this.matchService.setMatchActiveStatus(match.id!, newStatus).subscribe(() => {
      match.active = newStatus;
    });
  }

  // Extract date and time from datetime string for editing
  extractDateAndTime(dateTime: any): { date: string, time: string } {
    if (!dateTime) return { date: '', time: '' };

    const d = new Date(dateTime);
    
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

  resetForm(): void {
    this.matchForm.reset({ status: 'SCHEDULED', active: true });
    this.editingMatchId = null;
  }

  filteredMatches() {
    return this.matches
      .filter(m => {
        if (this.appliedStatusFilter === 'active') return m.active;
        if (this.appliedStatusFilter === 'inactive') return !m.active;
        return true;
      })
      .filter(m => {
        if (!this.appliedDateFilter) return true;
        return m.matchDate?.slice(0, 10) === this.appliedDateFilter;
      });
  }

  applyFilters() {
    this.appliedStatusFilter = this.statusFilter;
    this.appliedDateFilter = this.dateFilter;
  }

  resetFilters() {
    this.statusFilter = '';
    this.dateFilter = '';
    this.appliedStatusFilter = '';
    this.appliedDateFilter = '';
  }
  resetMatch(match: Match): void {
  if (!confirm(`Réinitialiser le match ${match.homeTeam} vs ${match.awayTeam} ?`)) {
    return;
  }

  const updatedMatch: Match = {
    ...match,
    status: 'SCHEDULED',
    homeScore: 0,
    awayScore: 0,
  };

  this.matchService.updateMatch(match.id!, updatedMatch).subscribe({
    next: () => {
      // Mettre à jour localement pour refléter le changement immédiat
      match.status = 'SCHEDULED';
      match.homeScore = 0;
      match.awayScore = 0;
      alert('Match réinitialisé avec succès.');
    },
    error: (err) => {
      console.error('Erreur lors de la réinitialisation du match', err);
      alert('Erreur lors de la réinitialisation. Veuillez réessayer.');
    }
  });
}
  allSelected(): boolean {
    const filtered = this.filteredMatches();
    return filtered.length > 0 && filtered.every(m => this.selectedMatchIds.has(m.id!));
  }

toggleSelectAll(event: Event): void {
  const input = event.target as HTMLInputElement;
  const checked = input.checked;
  const filtered = this.filteredMatches();
  if (checked) {
    filtered.forEach(m => this.selectedMatchIds.add(m.id!));
  } else {
    filtered.forEach(m => this.selectedMatchIds.delete(m.id!));
  }
}

onSelectionChange(match: Match, event: Event): void {
  const checked = (event.target as HTMLInputElement).checked;
  if (checked) {
    this.selectedMatchIds.add(match.id!);
  } else {
    this.selectedMatchIds.delete(match.id!);
  }
}

  getSelectedMatches(): Match[] {
    return this.matches.filter(m => this.selectedMatchIds.has(m.id!));
  }
  deleteSelectedMatches(): void {
  const selected = this.getSelectedMatches();
  if (selected.length === 0) return;

  if (!confirm(`Voulez-vous supprimer ${selected.length} match(s) sélectionné(s) ?`)) return;

  const deletes = selected.map(m => this.matchService.deleteMatch(m.id!).toPromise());

  Promise.all(deletes)
    .then(() => {
      alert(`${selected.length} match(s) supprimé(s) avec succès.`);
      this.selectedMatchIds.clear();
      this.loadMatches();
    })
    .catch(err => {
      console.error('Erreur lors de la suppression multiple', err);
      alert('Erreur lors de la suppression. Veuillez réessayer.');
    });
}

resetSelectedMatches(): void {
  const selected = this.getSelectedMatches();
  if (selected.length === 0) return;

  if (!confirm(`Voulez-vous réinitialiser ${selected.length} match(s) sélectionné(s) ?`)) return;

  const resets = selected.map(m => {
    const updatedMatch: Match = {
      ...m,
      status: 'SCHEDULED',
      homeScore: 0,
      awayScore: 0,
    };
    return this.matchService.updateMatch(m.id!, updatedMatch).toPromise();
  });

  Promise.all(resets)
    .then(() => {
      alert(`${selected.length} match(s) réinitialisé(s) avec succès.`);
      this.selectedMatchIds.clear();
      this.loadMatches();
    })
    .catch(err => {
      console.error('Erreur lors de la réinitialisation multiple', err);
      alert('Erreur lors de la réinitialisation. Veuillez réessayer.');
    });
}

}