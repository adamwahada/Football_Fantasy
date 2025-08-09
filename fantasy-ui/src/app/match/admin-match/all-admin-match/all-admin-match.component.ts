import { Component, OnInit, AfterViewInit, ViewChild } from '@angular/core';
import { MatchService, Match } from '../../match.service';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Router } from '@angular/router';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatTableDataSource } from '@angular/material/table';
import { ActivatedRoute } from '@angular/router';
import { FormControl } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { GameweekService } from '../../../gameweek/gameweek.service';
import { MatTooltipModule } from '@angular/material/tooltip';

interface MatchFilter {
  status?: string;
  date?: string;
  matchStatus?: string;
  dateRange?: string;
}

@Component({
  selector: 'app-admin-match',
  templateUrl: './all-admin-match.component.html',
  styleUrls: ['./all-admin-match.component.scss'],
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule,
    MatIconModule, MatButtonModule, MatCheckboxModule, MatPaginatorModule,MatTooltipModule,

     RouterLink, MatPaginatorModule,],
})
export class AllAdminMatchComponent implements OnInit, AfterViewInit {
  matches: Match[] = [];
  matchForm: FormGroup;
  editingMatchId: number | null = null;
  statuses = ['SCHEDULED', 'LIVE', 'COMPLETED', 'CANCELED'];
  selectedMatchIds = new Set<number>();
  dataSource = new MatTableDataSource<Match>();
  matchStatusFilter: string = '';
  gameweekId?: number;
  filteredMatches: Match[] = [];
  filterControl = new FormControl('all');

  @ViewChild(MatPaginator) paginator!: MatPaginator;

  // Filters
  statusFilter: string = '';
  dateFilter: string = '';
  appliedFilters: MatchFilter = {
    status: '',
    date: '',
    matchStatus: '',
    dateRange: 'all'
  };

  constructor(
    private matchService: MatchService,
    private fb: FormBuilder,
    private router: Router,
    private route: ActivatedRoute,
    private gameweekService: GameweekService
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
      gameweeks: this.fb.array([]),
    });
  }

  ngOnInit(): void {
    this.loadMatches();
    this.setupFilters();

    this.filterControl.valueChanges.subscribe(value => {
      this.dateFilter = ''; // Reset simple date filter when using date range
      this.appliedFilters.dateRange = value || 'all';
      this.applyFilters();
    });

    // Read gameweekId from route params if exists
    this.route.paramMap.subscribe(params => {
      const gwId = params.get('gameweekId');
      if (gwId) {
        this.gameweekId = +gwId;
      }
    });
  }

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
  }

  setupFilters(): void {
    this.dataSource.filterPredicate = (data: Match, filter: string): boolean => {
      if (!filter) return true;
      
      try {
        const filters: MatchFilter = JSON.parse(filter);
        
        // Status filter (active/inactive)
        const matchesStatus = filters.status
          ? (filters.status === 'active' ? !!data.active : !data.active)
          : true;

        // Specific date filter
        const matchesDate = filters.date
          ? new Date(data.matchDate).toDateString() === new Date(filters.date).toDateString()
          : true;

        // Match status filter (SCHEDULED, LIVE, etc.)
        const matchesMatchStatus = filters.matchStatus
          ? data.status === filters.matchStatus
          : true;

        // Date range filter (1 day, 3 days, 1 week)
        let matchesDateRange = true;
        if (filters.dateRange && filters.dateRange !== 'all') {
          const now = new Date();
          now.setHours(0, 0, 0, 0); // Start of today
          const matchDate = new Date(data.matchDate);
          matchDate.setHours(0, 0, 0, 0); // Start of match day

          switch (filters.dateRange) {
            case 'today':
              const today = new Date(now);
              today.setHours(0, 0, 0, 0); // début de la journée

              const tomorrowStart = new Date(today);
              tomorrowStart.setDate(tomorrowStart.getDate() + 1); // début de demain

              matchesDateRange = matchDate >= today && matchDate < tomorrowStart;
              break;
            case 'nextDay':
              const tomorrow = new Date(now);
              tomorrow.setDate(tomorrow.getDate() + 1);
              const dayAfterTomorrow = new Date(tomorrow);
              dayAfterTomorrow.setDate(dayAfterTomorrow.getDate() + 1);
              matchesDateRange = matchDate >= tomorrow && matchDate < dayAfterTomorrow;
              break;
              
            case 'next3Days':
              const threeDaysLater = new Date(now);
              threeDaysLater.setDate(threeDaysLater.getDate() + 3);
              matchesDateRange = matchDate >= now && matchDate < threeDaysLater;
              break;
              
            case 'nextWeek':
              const nextWeek = new Date(now);
              nextWeek.setDate(nextWeek.getDate() + 7);
              matchesDateRange = matchDate >= now && matchDate < nextWeek;
              break;
              
            default:
              matchesDateRange = true;
              break;
          }
        }

        return !!(matchesStatus && matchesDate && matchesMatchStatus && matchesDateRange);
      } catch (error) {
        console.error('Error parsing filter:', error);
        return true; // If parsing fails, show all data
      }
    };
  }

  loadMatches(): void {
    this.matchService.getAllMatches().subscribe((data) => {
      this.matches = data;
      this.dataSource.data = data;
      if (this.paginator) {
        this.dataSource.paginator = this.paginator;
      }
      this.applyFilters();
    });
  }

  applyFilters(): void {
    this.appliedFilters = {
      status: this.statusFilter,
      date: this.dateFilter,
      matchStatus: this.matchStatusFilter,
      dateRange: this.filterControl.value || 'all'
    };

    this.dataSource.filter = JSON.stringify(this.appliedFilters);

    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }

    this.selectedMatchIds.clear();
  }

  resetFilters(): void {
    this.statusFilter = '';
    this.dateFilter = '';
    this.matchStatusFilter = '';
    this.filterControl.setValue('all');

    this.appliedFilters = {
      status: '',
      date: '',
      matchStatus: '',
      dateRange: 'all'
    };

    this.dataSource.filter = '';
    this.selectedMatchIds.clear();
  }

  getPagedData(): Match[] {
    if (!this.dataSource.paginator) {
      return this.dataSource.filteredData;
    }
    
    const startIndex = this.dataSource.paginator.pageIndex * this.dataSource.paginator.pageSize;
    const endIndex = startIndex + this.dataSource.paginator.pageSize;
    return this.dataSource.filteredData.slice(startIndex, endIndex);
  }

  allSelected(): boolean {
    const pagedData = this.getPagedData();
    return pagedData.length > 0 && pagedData.every(m => this.selectedMatchIds.has(m.id!));
  }

  toggleSelectAll(event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    const pagedData = this.getPagedData();
    pagedData.forEach(m => 
      checked ? this.selectedMatchIds.add(m.id!) : this.selectedMatchIds.delete(m.id!)
    );
  }

  onSelectionChange(match: Match, event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    checked ? this.selectedMatchIds.add(match.id!) : this.selectedMatchIds.delete(match.id!);
  }

  getSelectedMatches(): Match[] {
    return this.matches.filter(m => this.selectedMatchIds.has(m.id!));
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
      match.finished = false;
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

  linkSelectedMatchesToGameweek(): void {
    if (!this.gameweekId) return;

    const matchIds = Array.from(this.selectedMatchIds);
    this.gameweekService.linkMultipleMatches(this.gameweekId, matchIds).subscribe({
      next: () => {
        alert(`${matchIds.length} matches linked to Gameweek ${this.gameweekId}`);
        this.selectedMatchIds.clear();
        this.loadMatches();
        // Optionally navigate back to gameweek list or details page
        this.router.navigate(['/admin/allgameweek', this.gameweekId]);
      },
      error: (err) => {
        console.error('Error linking matches to gameweek', err);
        alert('Failed to link matches. Please try again.');
      }
    });
  }
  
  removeFilter(filterName: 'statusFilter' | 'dateFilter' | 'matchStatusFilter' | 'dateRange'): void {
  switch (filterName) {
    case 'statusFilter':
      this.statusFilter = '';
      break;
    case 'dateFilter':
      this.dateFilter = '';
      break;
    case 'matchStatusFilter':
      this.matchStatusFilter = '';
      break;
    case 'dateRange':
      this.filterControl.setValue('all', { emitEvent: false }); 
      break;
  }
  this.applyFilters();
}
addNew(): void {
  // Navigate to add match page
  this.router.navigate(['/admin/Addmatch']);
}

}