import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { MatchService,Match } from '../../match.service';
@Component({
  selector: 'app-admin-match',
  templateUrl: './add-admin-match.component.html',
  styleUrls: ['./add-admin-match.component.scss'],
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
})
export class AddAdminMatchComponent implements OnInit {
  matches: Match[] = [];
  matchForm: FormGroup;
  editingMatchId: number | null = null;
  statuses = ['SCHEDULED', 'LIVE', 'COMPLETED', 'CANCELED'];

  // Filtres
  statusFilter: string = '';
  dateFilter: string = '';
  appliedStatusFilter: string = '';
  appliedDateFilter: string = '';

  constructor(private matchService: MatchService, private fb: FormBuilder) {
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
}