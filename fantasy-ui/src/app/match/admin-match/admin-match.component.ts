import { Component, OnInit } from '@angular/core';
import { Match, MatchService } from '../match.service';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router'; 
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';

@Component({
  selector: 'app-admin-match',
  templateUrl: './admin-match.component.html',
  styleUrls: ['./admin-match.component.scss'],
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
})
export class AdminMatchComponent implements OnInit {
  matches: Match[] = [];
  matchForm: FormGroup;
  editingMatchId: number | null = null;
  statuses = ['SCHEDULED', 'LIVE', 'COMPLETED', 'CANCELED'];

  // Ajoute ces propriétés :
  statusFilter: string = '';
  dateFilter: string = '';

  constructor(private matchService: MatchService, private fb: FormBuilder) {
    this.matchForm = this.fb.group({
      homeTeam: ['', Validators.required],
      awayTeam: ['', Validators.required],
      matchDate: ['', Validators.required],
      homeScore: [null],
      awayScore: [null],
      description: [''],
      status: ['SCHEDULED', Validators.required],
      active: [true],
      gameweeks: this.fb.array([]), // Ajouter dynamiquement si nécessaire
    });
  }

  ngOnInit(): void {
    this.loadMatches();
  }

  loadMatches(): void {
    this.matchService.getAllMatches().subscribe((data) => (this.matches = data));
  }

  onSubmit(): void {
    if (this.matchForm.invalid) return;

    const match: Match = this.matchForm.value;

    if (this.editingMatchId) {
      this.matchService.updateMatch(this.editingMatchId, match).subscribe(() => {
        this.loadMatches();
        this.resetForm();
      });
    } else {
      this.matchService.createMatch(match).subscribe(() => {
        this.loadMatches();
        this.resetForm();
      });
    }
  }

  editMatch(match: Match): void {
    this.editingMatchId = match.id!;
    this.matchForm.patchValue(match);
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

  resetForm(): void {
    this.matchForm.reset({ status: 'SCHEDULED', active: true });
    this.editingMatchId = null;
  }

  // Ajoute cette méthode :
  filteredMatches() {
    return this.matches
      .filter(m => {
        if (this.statusFilter === 'active') return m.active;
        if (this.statusFilter === 'inactive') return !m.active;
        return true;
      })
      .filter(m => {
        if (!this.dateFilter) return true;
        return m.matchDate.startsWith(this.dateFilter);
      });
  }

  resetFilters() {
    this.statusFilter = '';
    this.dateFilter = '';
  }
}
