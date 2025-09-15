import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { GameweekService, Gameweek } from '../../gameweek.service';
import { Match, MatchWithIconsDTO } from '../../../match/match.service';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatTableDataSource } from '@angular/material/table';
import { AfterViewInit, ViewChild } from '@angular/core';
import { MatchService } from '../../../match/match.service';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ShowGameweekMatchesComponent } from '../show-gameweek-matches/show-gameweek-matches.component';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-all-admin-gameweek',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, 
    MatPaginatorModule, MatPaginator,MatIconModule,
    MatButtonModule, MatTooltipModule,
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

  refreshing = false;

  constructor(
    private gameweekService: GameweekService,
    private fb: FormBuilder,
    private router: Router,
    private matchService: MatchService,
    private route: ActivatedRoute,
    private snackBar: MatSnackBar // <-- inject MatSnackBar
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
  this.setupFilters();
  
  // Charger les gameweeks ET vérifier les query params après
  this.loadGameweeks().then(() => {
    this.checkForModalToOpen();
  });
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

loadGameweeks(): Promise<void> {
  return new Promise((resolve) => {
    this.gameweekService.getAllGameweeks().subscribe((data: Gameweek[]) => {
      this.gameweeks = data;
      this.dataSource.data = data;

      if (this.paginator) {
        this.dataSource.paginator = this.paginator;
      }

      this.applyFilters();

      // ✅ Load matches count for each gameweek
      this.loadGameweekCounts(this.gameweeks).then(() => {
        console.log('✅ Gameweek counts loaded');
        resolve();
      });

      console.log('✅ Gameweeks loaded:', data.length);
    });
  });
}


// Nouvelle méthode pour vérifier les query params
private checkForModalToOpen(): void {
  this.route.queryParams.subscribe(params => {
    const gameweekIdToOpen = params['openModal'];
    if (gameweekIdToOpen) {
      console.log('✅ Query param found - gameweek ID to open:', gameweekIdToOpen);
      
      // Trouver la gameweek par ID
      const gameweekToOpen = this.gameweeks.find(gw => gw.id == gameweekIdToOpen);
      if (gameweekToOpen) {
        console.log('✅ Gameweek found:', gameweekToOpen);
        
        // Ouvrir le modal après un petit délai
        setTimeout(() => {
          this.showMatches(gameweekToOpen);
        }, 200);
        
        // Nettoyer l'URL
        this.router.navigate([], {
          relativeTo: this.route,
          queryParams: {},
          replaceUrl: true
        });
      } else {
        console.error('❌ Gameweek not found with ID:', gameweekIdToOpen);
      }
    } else {
      console.log('🔍 No openModal query param found');
    }
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
  addNew() {
  this.router.navigate(['/admin/AddGameweek']);
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
  selectMatches(gameweekId: number): void {
  this.router.navigate(['/admin/Allmatch/select', gameweekId]);
}
resetGameweek(gameweekId: number): void {
  if (!confirm('Voulez-vous réinitialiser tous les matchs de ce gameweek ?')) {
    return;
  }

  this.gameweekService.getMatchesByGameweek(gameweekId).subscribe({
    next: (matches) => {
      const resets = matches.map(m => {
        const updatedMatch: Match = {
          ...m,
          status: 'SCHEDULED', // ✅ OK if 'SCHEDULED' is in the Match type
          homeScore: 0,
          awayScore: 0
        };
        return this.matchService.updateMatch(m.id!, updatedMatch).toPromise();
      });

      Promise.all(resets)
        .then(() => {
          alert(`Tous les matchs du gameweek ${gameweekId} ont été réinitialisés.`);
        })
        .catch(err => {
          console.error('Erreur lors de la réinitialisation', err);
          alert('Erreur lors de la réinitialisation. Veuillez réessayer.');
        });
    },
    error: (err) => {
      console.error('Erreur récupération des matchs du gameweek', err);
      alert('Impossible de récupérer les matchs du gameweek.');
    }
  });
}

getMatchesCount(gameweek: Gameweek): number {
  return (gameweek as any).matchesCount || 0;
}

getTiebreakerCount(gameweek: Gameweek): number {
  return (gameweek as any).tiebreakerCount || 0;
}
private loadGameweekCounts(gameweeks: Gameweek[]): Promise<void> {
    return new Promise((resolve) => {
      if (gameweeks.length === 0) {
        resolve();
        return;
      }

      let completed = 0;
      const total = gameweeks.length;

      gameweeks.forEach(gameweek => {
        if (gameweek.id) {
          // Charger le nombre de matchs
          this.gameweekService.getMatchesByGameweek(gameweek.id).subscribe({
            next: (matches) => {
              // ✅ Compter uniquement les matchs actifs
              const activeCount = matches.filter((m: any) => m && (m.active === true || m.active === undefined)).length;
              (gameweek as any).matchesCount = activeCount;
              
              // Calculer les tiebreakers à partir de tiebreakerMatchIds
              if (gameweek.tiebreakerMatchIds) {
                const tiebreakerIds = gameweek.tiebreakerMatchIds.split(',').filter(id => id.trim());
                (gameweek as any).tiebreakerCount = tiebreakerIds.length;
              } else {
                (gameweek as any).tiebreakerCount = 0;
              }
              
              completed++;
              if (completed === total) {
                resolve();
              }
            },
            error: (error) => {
              console.error(`Error loading matches count for gameweek ${gameweek.id}:`, error);
              (gameweek as any).matchesCount = 0;
              (gameweek as any).tiebreakerCount = 0;
              completed++;
              if (completed === total) {
                resolve();
              }
            }
          });
        } else {
          completed++;
          if (completed === total) {
            resolve();
          }
        }
      });
    });
  }

onMatchesUpdated(data: {gameweek: Gameweek, matches: MatchWithIconsDTO[]}): void {
  console.log('✅ Received matches update from child component');
  
  // Update the matches array for the modal
  this.matchesForSelectedGameweek = data.matches;
  
  // Update the selected gameweek with fresh data
  this.selectedGameweekForMatches = data.gameweek;
  
  // Find and update the gameweek in your main data source
  const gameweekIndex = this.dataSource.data.findIndex(gw => gw.id === data.gameweek.id);
  if (gameweekIndex !== -1) {
    // Update the gameweek in the data source
    this.dataSource.data[gameweekIndex] = data.gameweek;
    
    // Recalculate counts for this specific gameweek
    this.updateSingleGameweekCounts(data.gameweek, data.matches);
    
    // Trigger change detection for the table
    this.dataSource._updateChangeSubscription();
  }
  
  // Also update the gameweeks array
  const mainGameweekIndex = this.gameweeks.findIndex(gw => gw.id === data.gameweek.id);
  if (mainGameweekIndex !== -1) {
    this.gameweeks[mainGameweekIndex] = data.gameweek;
  }
}

// Helper method to update counts for a single gameweek
private updateSingleGameweekCounts(gameweek: Gameweek, matches: MatchWithIconsDTO[]): void {
  // Update matches count (only active ones)
  const activeCount = matches.filter((m: any) => m && (m.active === true || m.active === undefined)).length;
  (gameweek as any).matchesCount = activeCount;
  
  // Update tiebreaker count
  if (gameweek.tiebreakerMatchIds) {
    const tiebreakerIds = gameweek.tiebreakerMatchIds.split(',').filter(id => id.trim());
    (gameweek as any).tiebreakerCount = tiebreakerIds.length;
  } else {
    (gameweek as any).tiebreakerCount = 0;
  }
  
  console.log('✅ Updated counts for gameweek', gameweek.id, {
    matches: (gameweek as any).matchesCount,
    tiebreakers: (gameweek as any).tiebreakerCount
  });
}

onSeedGameweek(gameweek: Gameweek): void {
  if (!gameweek.competition || !gameweek.weekNumber) {
    alert('Competition or week number missing!');
    return;
  }

  this.gameweekService.seedGameweek(gameweek.competition, gameweek.weekNumber).subscribe({
    next: (res) => {
      alert(`✅ ${res}`);
      this.loadGameweeks(); // refresh table
    },
    error: (err) => {
      console.error('❌ Error seeding gameweek:', err);
      alert(err.error || 'Erreur lors du seed.');
    }
  });
}
onUpdateAllMatches(): void {
  this.gameweekService.updateMatchesNow().subscribe({
    next: (res) => {
      alert(`✅ ${res}`);
      this.loadGameweeks(); // refresh table after update
    },
    error: (err) => {
      console.error('❌ Error updating matches:', err);
      alert(err.error || 'Erreur lors de la mise à jour des matchs.');
    }
  });
}

onUpdateSpecificGameweek(competition: string, weekNumber: number): void {
  this.gameweekService.updateSpecificGameweek(competition, weekNumber).subscribe({
    next: (res) => {
      alert(`✅ ${res}`);
      this.loadGameweeks(); // refresh table after update
    },
    error: (err) => {
      console.error('❌ Error updating gameweek:', err);
      alert(err.error || 'Erreur lors de la mise à jour du gameweek.');
    }
  });
}

onRefresh(): void {
  this.refreshing = true;
  this.loadGameweeks().then(() => {
    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }

    this.snackBar.open('Refreshed !', 'Fermer', {
      duration: 3000,
      verticalPosition: 'top',  // ⬅️ Snackbar at the top
      horizontalPosition: 'center' // optional, centers horizontally
    });

    setTimeout(() => {
      this.refreshing = false;
    }, 1200);
  });
}

// ✅ Validate a gameweek
validateGameweek(gameweek: Gameweek): void {
  if (!gameweek.id) return;
  
  this.gameweekService.validateGameweek(gameweek.id).subscribe({
    next: (updatedGameweek) => {
      // Update the gameweek in the local array
      const index = this.gameweeks.findIndex(gw => gw.id === gameweek.id);
      if (index !== -1) {
        // Conserver les champs dynamiques (matchesCount, tiebreakerCount), puis rafraîchir
        const prev = this.gameweeks[index] as any;
        this.gameweeks[index] = { ...(updatedGameweek as any), matchesCount: prev.matchesCount, tiebreakerCount: prev.tiebreakerCount } as Gameweek;
        this.dataSource.data = this.gameweeks;
        this.refreshCountsForGameweek(gameweek.id);
      }
      
      this.snackBar.open(`✅ Gameweek ${gameweek.weekNumber} validée avec succès!`, 'Fermer', {
        duration: 3000,
        verticalPosition: 'top',
        horizontalPosition: 'center'
      });
    },
    error: (err) => {
      console.error('❌ Error validating gameweek:', err);
      this.snackBar.open('❌ Erreur lors de la validation de la gameweek', 'Fermer', {
        duration: 3000,
        verticalPosition: 'top',
        horizontalPosition: 'center'
      });
    }
  });
}

// ✅ Unvalidate a gameweek
unvalidateGameweek(gameweek: Gameweek): void {
  if (!gameweek.id) return;
  
  this.gameweekService.unvalidateGameweek(gameweek.id).subscribe({
    next: (updatedGameweek) => {
      // Update the gameweek in the local array
      const index = this.gameweeks.findIndex(gw => gw.id === gameweek.id);
      if (index !== -1) {
        // Conserver les champs dynamiques (matchesCount, tiebreakerCount), puis rafraîchir
        const prev = this.gameweeks[index] as any;
        this.gameweeks[index] = { ...(updatedGameweek as any), matchesCount: prev.matchesCount, tiebreakerCount: prev.tiebreakerCount } as Gameweek;
        this.dataSource.data = this.gameweeks;
        this.refreshCountsForGameweek(gameweek.id);
      }
      
      this.snackBar.open(`✅ Gameweek ${gameweek.weekNumber} invalidée avec succès!`, 'Fermer', {
        duration: 3000,
        verticalPosition: 'top',
        horizontalPosition: 'center'
      });
    },
    error: (err) => {
      console.error('❌ Error unvalidating gameweek:', err);
      this.snackBar.open('❌ Erreur lors de l\'invalidation de la gameweek', 'Fermer', {
        duration: 3000,
        verticalPosition: 'top',
        horizontalPosition: 'center'
      });
    }
  });
}

// ✅ Check if gameweek is validated (boolean flag from backend)
isGameweekValidated(gameweek: Gameweek): boolean {
  return !!(gameweek as any).validated;
}

// 🔄 Recharger le nombre de matchs pour une gameweek spécifique
private refreshCountsForGameweek(gameweekId?: number): void {
  if (!gameweekId) return;
  this.gameweekService.getMatchesByGameweek(gameweekId).subscribe({
    next: (matches) => {
      const idx = this.gameweeks.findIndex(gw => gw.id === gameweekId);
      if (idx !== -1) {
        const activeCount = matches.filter((m: any) => m && (m.active === true || m.active === undefined)).length;
        (this.gameweeks[idx] as any).matchesCount = activeCount;
        // tiebreakers: recalculer à partir du champ string si présent
        const tbIds = (this.gameweeks[idx] as any).tiebreakerMatchIds as string | undefined;
        (this.gameweeks[idx] as any).tiebreakerCount = tbIds ? tbIds.split(',').filter(id => id.trim()).length : 0;
        this.dataSource.data = [...this.gameweeks];
      }
    },
    error: () => {
      const idx = this.gameweeks.findIndex(gw => gw.id === gameweekId);
      if (idx !== -1) {
        (this.gameweeks[idx] as any).matchesCount = 0;
        this.dataSource.data = [...this.gameweeks];
      }
    }
  });
}

}