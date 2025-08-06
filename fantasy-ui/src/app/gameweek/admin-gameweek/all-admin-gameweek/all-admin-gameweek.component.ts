import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { GameweekService, Gameweek } from '../../gameweek.service';
import { MatchWithIconsDTO } from '../../../match/match.service';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatTableDataSource } from '@angular/material/table';
import { AfterViewInit, ViewChild } from '@angular/core';
import { ShowGameweekMatchesComponent } from '../show-gameweek-matches/show-gameweek-matches.component';

@Component({
  selector: 'app-all-admin-gameweek',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, 
    MatPaginatorModule, MatPaginator,
    RouterLink, ShowGameweekMatchesComponent],
  templateUrl: './all-admin-gameweek.component.html',
  styleUrls: ['./all-admin-gameweek.component.scss']
})
export class AllAdminGameweekComponent implements OnInit, AfterViewInit {
  gameweeks: Gameweek[] = [];
  gameweekForm: FormGroup;
  editingGameweekId: number | null = null;
  selectedGameweekIds = new Set<number>();
  dataSource = new MatTableDataSource<Gameweek>();

  @ViewChild(MatPaginator) paginator!: MatPaginator;

  statusFilter: string = '';
  competitionFilter: string = '';
  searchFilter: string = ''; // Add search filter

  competitions = [
    { value: 'PREMIER_LEAGUE', label: 'Premier League' },
    { value: 'LA_LIGA', label: 'La Liga' },
    { value: 'SERIE_A', label: 'Serie A' },
    { value: 'BUNDESLIGA', label: 'Bundesliga' },
    { value: 'LIGUE_ONE', label: 'Ligue 1' },
    { value: 'CHAMPIONS_LEAGUE', label: 'Champions League' },
    { value: 'EUROPA_LEAGUE', label: 'Europa League' }
  ];

  statuses = [
    { value: 'UPCOMING', label: 'Upcoming' },
    { value: 'ONGOING', label: 'Ongoing' },
    { value: 'FINISHED', label: 'Finished' },
    { value: 'CANCELLED', label: 'Cancelled' }
  ];

  // ✅ Show Matches UI State
  matchesForSelectedGameweek: MatchWithIconsDTO[] = [];
  selectedGameweekForMatches: Gameweek | null = null;
  loadingMatches: boolean = false;
  matchesLoadError: string | null = null;

  constructor(
    private gameweekService: GameweekService,
    private fb: FormBuilder,
    private router: Router
  ) {
    this.gameweekForm = this.fb.group({
      weekNumber: ['', [Validators.required, Validators.min(1), Validators.max(50)]],
      status: ['UPCOMING', Validators.required],
      competition: ['', Validators.required],
      startDate: ['', Validators.required],
      startTime: ['', Validators.required],
      endDate: ['', Validators.required],
      endTime: ['', Validators.required],
      joinDeadline: ['', Validators.required],
      joinDeadlineTime: ['', Validators.required],
      description: ['']
    });
  }

  ngOnInit(): void {
    this.loadGameweeks();
    this.setupFilters();
  }

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
  }

  setupFilters(): void {
    // Set up custom filter predicate
    this.dataSource.filterPredicate = (data: Gameweek, filter: string): boolean => {
      const filterObj = JSON.parse(filter);
      
      // Status filter
      const statusMatch = !filterObj.status || data.status === filterObj.status;
      
      // Competition filter
      const competitionMatch = !filterObj.competition || data.competition === filterObj.competition;
      
      // Search filter (searches across multiple fields)
      const searchMatch = !filterObj.search || 
        data.weekNumber.toString().toLowerCase().includes(filterObj.search.toLowerCase()) ||
        data.competition.toLowerCase().includes(filterObj.search.toLowerCase()) ||
        data.status.toLowerCase().includes(filterObj.search.toLowerCase()) ||
        (data.description ? data.description.toLowerCase().includes(filterObj.search.toLowerCase()) : false);
      
      return statusMatch && competitionMatch && searchMatch;
    };
  }

loadGameweeks(): void {
  this.gameweekService.getAllGameweeks().subscribe((data: Gameweek[]) => {
    this.gameweeks = data;
    this.dataSource.data = data;
    // Make sure paginator is connected after data is loaded
    if (this.paginator) {
      this.dataSource.paginator = this.paginator;
    }
    this.applyFilters(); // Apply current filters
  });
}
getPagedData(): Gameweek[] {
  if (!this.dataSource.paginator) {
    return this.dataSource.filteredData;
  }
  
  const startIndex = this.dataSource.paginator.pageIndex * this.dataSource.paginator.pageSize;
  const endIndex = startIndex + this.dataSource.paginator.pageSize;
  return this.dataSource.filteredData.slice(startIndex, endIndex);
}

  applyFilters(): void {
    const filterValue = {
      status: this.statusFilter,
      competition: this.competitionFilter,
      search: this.searchFilter
    };
    
    this.dataSource.filter = JSON.stringify(filterValue);
    
    // Reset paginator to first page when filtering
    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
    
    // Clear selections when filters change
    this.selectedGameweekIds.clear();
  }

  resetFilters(): void {
    this.statusFilter = '';
    this.competitionFilter = '';
    this.searchFilter = '';
    this.applyFilters();
  }

  applySearchFilter(event: Event): void {
    const filterValue = (event.target as HTMLInputElement).value;
    this.searchFilter = filterValue;
    this.applyFilters();
  }

  // Update selection methods to work with filtered data
allSelected(): boolean {
  const pagedData = this.getPagedData();
  return pagedData.length > 0 && pagedData.every(gw => this.selectedGameweekIds.has(gw.id!));
}

toggleSelectAll(event: Event): void {
  const checked = (event.target as HTMLInputElement).checked;
  const pagedData = this.getPagedData();
  pagedData.forEach(gw => 
    checked ? this.selectedGameweekIds.add(gw.id!) : this.selectedGameweekIds.delete(gw.id!)
  );
}

  onSelectionChange(gameweek: Gameweek, event: Event): void {
    const checked = (event.target as HTMLInputElement).checked;
    checked ? this.selectedGameweekIds.add(gameweek.id!) : this.selectedGameweekIds.delete(gameweek.id!);
  }

  getSelectedGameweeks(): Gameweek[] {
    return this.gameweeks.filter(gw => this.selectedGameweekIds.has(gw.id!));
  }

  deleteSelectedGameweeks(): void {
    const selected = this.getSelectedGameweeks();
    if (!selected.length) return;
    if (!confirm(`Supprimer ${selected.length} gameweek(s) ?`)) return;

    const deletes = selected.map(gw => this.gameweekService.deleteGameweek(gw.id!).toPromise());
    Promise.all(deletes).then(() => {
      alert(`Supprimé(s) avec succès.`);
      this.selectedGameweekIds.clear();
      this.loadGameweeks();
    }).catch(err => {
      console.error('Erreur suppression multiple', err);
      alert('Erreur suppression.');
    });
  }

  onSubmit(): void {
    if (this.gameweekForm.invalid) return;

    const formValue = this.gameweekForm.value;
    const startDateTime = `${formValue.startDate}T${formValue.startTime}:00`;
    const endDateTime = `${formValue.endDate}T${formValue.endTime}:00`;
    const joinDeadlineDateTime = `${formValue.joinDeadline}T${formValue.joinDeadlineTime}:00`;

    const gameweek: Gameweek = {
      ...formValue,
      startDate: startDateTime,
      endDate: endDateTime,
      joinDeadline: joinDeadlineDateTime
    };

    delete (gameweek as any).startTime;
    delete (gameweek as any).endTime;
    delete (gameweek as any).joinDeadlineTime;

    if (this.editingGameweekId) {
      this.gameweekService.updateGameweek(this.editingGameweekId, gameweek).subscribe({
        next: () => {
          this.loadGameweeks();
          this.resetForm();
        },
        error: (error) => console.error('Error updating gameweek:', error)
      });
    } else {
      this.gameweekService.createGameweek(gameweek).subscribe({
        next: () => {
          this.loadGameweeks();
          this.resetForm();
        },
        error: (error) => console.error('Error creating gameweek:', error)
      });
    }
  }

  editGameweek(gameweek: Gameweek): void {
    this.editingGameweekId = gameweek.id!;
    const start = this.extractDateAndTime(gameweek.startDate);
    const end = this.extractDateAndTime(gameweek.endDate);
    const deadline = this.extractDateAndTime(gameweek.joinDeadline);

    this.gameweekForm.patchValue({
      ...gameweek,
      startDate: start.date,
      startTime: start.time,
      endDate: end.date,
      endTime: end.time,
      joinDeadline: deadline.date,
      joinDeadlineTime: deadline.time
    });
  }

  deleteGameweek(id: number): void {
    if (confirm('Voulez-vous supprimer cette gameweek ?')) {
      this.gameweekService.deleteGameweek(id).subscribe(() => this.loadGameweeks());
    }
  }

  extractDateAndTime(dateTime: any): { date: string, time: string } {
    const d = new Date(dateTime);
    return {
      date: d.toISOString().split('T')[0],
      time: d.toTimeString().slice(0, 5)
    };
  }

  resetForm(): void {
    this.gameweekForm.reset({ status: 'UPCOMING' });
    this.editingGameweekId = null;
  }

  showMatches(gameweek: Gameweek): void {
    this.selectedGameweekForMatches = gameweek;
    this.loadingMatches = true;
    this.matchesLoadError = null;
    this.matchesForSelectedGameweek = [];

    this.gameweekService.getMatchesWithIcons(gameweek.id!).subscribe({
      next: (matches) => {
        this.matchesForSelectedGameweek = matches;
        this.loadingMatches = false;
      },
      error: (err) => {
        this.matchesLoadError = 'Erreur lors du chargement des matchs.';
        this.loadingMatches = false;
        console.error(err);
      }
    });
  }

  closeMatchesModal(): void {
    this.selectedGameweekForMatches = null;
    this.matchesForSelectedGameweek = [];
    this.matchesLoadError = null;
  }

  getTimeLeft(deadline: string): string {
    if (!deadline) return '';

    const now = new Date();
    const end = new Date(deadline);
    const diff = end.getTime() - now.getTime();

    if (diff <= 0) {
      return 'Deadline passed';
    }

    const days = Math.floor(diff / (1000 * 60 * 60 * 24));
    const hours = Math.floor((diff / (1000 * 60 * 60)) % 24);
    const minutes = Math.floor((diff / (1000 * 60)) % 60);

    let result = `Days: ${days}`;

    if (days < 7) {
      result += ` Hrs: ${hours}`;
      if (hours < 24) {
        result += ` Min: ${minutes}`;
      }
    }

    return result;
  }

  getRowClass(status: string): string {
    switch (status) {
      case 'FINISHED':
        return 'row-finished'; 
      case 'ONGOING':
        return 'row-ongoing';  
      case 'CANCELLED':
        return 'row-cancelled'; 
      default:
        return '';
    }
  }

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString('fr-FR', {
      year: 'numeric', month: 'short', day: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }

  formatCompetition(competition: string): string {
    return this.competitions.find(c => c.value === competition)?.label || competition.replace(/_/g, ' ');
  }

  getStatusClass(status: string): string {
    const map: { [key: string]: string } = {
      'UPCOMING': 'badge bg-primary',
      'ACTIVE': 'badge bg-success',
      'FINISHED': 'badge bg-secondary',
      'CANCELLED': 'badge bg-danger'
    };
    return map[status] || 'badge bg-light';
  }

  trackByGameweek(index: number, gameweek: Gameweek): number {
    return gameweek.id!;
  }
}