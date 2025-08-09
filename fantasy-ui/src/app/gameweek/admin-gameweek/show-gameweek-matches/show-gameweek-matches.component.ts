import { Component, Input, Output, EventEmitter, OnChanges, SimpleChanges } from '@angular/core';
import { MatchWithIconsDTO } from '../../../match/match.service';
import { Gameweek, GameweekService } from '../../gameweek.service';
import { MatchService } from '../../../match/match.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-show-gameweek-matches',
  templateUrl: './show-gameweek-matches.component.html',
  styleUrls: ['./show-gameweek-matches.component.scss'],
  standalone: true,
  imports: [CommonModule]
})
export class ShowGameweekMatchesComponent implements OnChanges {
  @Input() gameweek!: Gameweek;
  @Input() matches: MatchWithIconsDTO[] = [];

  private _matches: (MatchWithIconsDTO & { isTiebreaker?: boolean })[] = [];

  // Properties for selection mode
  isSelectionMode = false;
  selectedMatches: Set<number> = new Set();

  @Output() close = new EventEmitter<void>();
  @Output() matchesUpdated = new EventEmitter<{gameweek: Gameweek, matches: MatchWithIconsDTO[]}>();

  readonly baseUrl = 'http://localhost:9090/fantasy';

  constructor(
    private gameweekService: GameweekService,
    private matchService: MatchService
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['matches'] || changes['gameweek']) {
      this.updateMatchesWithTiebreakers();
    }
  }

  private updateMatchesWithTiebreakers(): void {
    let tiebreakerIds: number[] = [];

    if (Array.isArray(this.gameweek?.tiebreakerMatchIds)) {
      tiebreakerIds = this.gameweek.tiebreakerMatchIds as unknown as number[];
    } else if (typeof this.gameweek?.tiebreakerMatchIds === 'string') {
      tiebreakerIds = this.gameweek.tiebreakerMatchIds
        .split(',')
        .map(id => Number(id.trim()))
        .filter(id => !isNaN(id));
    }

    this._matches = this.matches.map(m => ({
      ...m,
      // Only mark as tiebreaker if the match is active AND in the tiebreaker list
      isTiebreaker: m.id != null && m.active && tiebreakerIds.includes(m.id)
    }));
  }

  get matchesWithTiebreaker(): (MatchWithIconsDTO & { isTiebreaker?: boolean })[] {
    return this._matches;
  }

  formatCompetition(comp?: string): string {
    return comp?.replace(/_/g, ' ') || '';
  }

  getFullIconUrl(relativePath: string): string {
    if (!relativePath) return `${this.baseUrl}/assets/images/teams/default.png`;
    if (relativePath.startsWith('http')) return relativePath;
    return `${this.baseUrl}${relativePath}`;
  }

  getMatchStatus(match: any): string {
    if (!match.active || match.active === false) return 'Inactif';
    if (match.finished) return 'Fini';

    switch (match.status) {
      case 'LIVE': return 'En cours';
      case 'COMPLETED': return 'Fini';
      case 'CANCELED': return 'Annulé';
      case 'SCHEDULED':
      default: return 'À venir';
    }
  }

  removeMatch(matchId: number): void {
    if (!this.gameweek?.id) return;

    if (!confirm('Voulez-vous vraiment retirer ce match de cette gameweek ?')) return;

    const wasThebreaker = this.isMatchTiebreaker(matchId);

    this.gameweekService.deleteSelectedMatches(this.gameweek.id, [matchId]).subscribe({
      next: () => {
        this._matches = this._matches.filter(m => m.id !== matchId);
        this.matches = this.matches.filter(m => m.id !== matchId);
        
        // If removed match was a tiebreaker, update tiebreakers
        if (wasThebreaker) {
          this.removeTiebreakerAndUpdate(matchId);
        } else {
          // Emit updated matches to parent
          this.matchesUpdated.emit({gameweek: this.gameweek, matches: this.matches});
        }
      },
      error: err => {
        console.error('Erreur lors de la suppression du match :', err);
        alert('Erreur lors de la suppression du match.');
      }
    });
  }

  toggleActive(match: MatchWithIconsDTO): void {
    const newStatus = !match.active;
    const wasThebreaker = this.isMatchTiebreaker(match.id!);
    
    this.matchService.setMatchActiveStatus(match.id!, newStatus).subscribe({
      next: () => {
        // Update the match in both arrays
        match.active = newStatus;
        const matchInArray = this._matches.find(m => m.id === match.id);
        if (matchInArray) {
          matchInArray.active = newStatus;
        }
        const originalMatch = this.matches.find(m => m.id === match.id);
        if (originalMatch) {
          originalMatch.active = newStatus;
        }

        // If deactivating a tiebreaker match, immediately update tiebreakers
        if (!newStatus && wasThebreaker) {
          this.removeTiebreakerAndUpdate(match.id!);
        } else {
          // Just refresh the tiebreaker status display
          this.updateMatchesWithTiebreakers();
          // Emit updated matches to parent
          this.matchesUpdated.emit({gameweek: this.gameweek, matches: this.matches});
        }
      },
      error: err => {
        console.error('Erreur lors de la mise à jour du statut du match :', err);
        alert('Erreur lors de la mise à jour du statut du match.');
      }
    });
  }

  /**
   * Helper method to check if a match is currently a tiebreaker
   */
  private isMatchTiebreaker(matchId: number): boolean {
    let currentTiebreakerIds: number[] = [];

    if (Array.isArray(this.gameweek?.tiebreakerMatchIds)) {
      currentTiebreakerIds = this.gameweek.tiebreakerMatchIds as unknown as number[];
    } else if (typeof this.gameweek?.tiebreakerMatchIds === 'string') {
      currentTiebreakerIds = this.gameweek.tiebreakerMatchIds
        .split(',')
        .map(id => Number(id.trim()))
        .filter(id => !isNaN(id));
    }

    return currentTiebreakerIds.includes(matchId);
  }

  /**
   * Remove a specific match from tiebreakers using the new backend endpoint
   */
  private removeTiebreakerAndUpdate(matchId: number): void {
    if (!this.gameweek?.id) return;

    // Use the new backend endpoint to remove the match from tiebreakers
    this.gameweekService.deleteTiebreakerMatches(this.gameweek.id, [matchId]).subscribe({
      next: () => {
        console.log(`Match ${matchId} removed from tiebreakers successfully.`);
        
        // Reload fresh data to get updated tiebreaker list from backend
        this.reloadGameweekAndMatches();
      },
      error: err => {
        console.error('Erreur lors de la suppression du tiebreaker :', err);
        alert('Erreur lors de la suppression du tiebreaker.');
      }
    });
  }

  startTiebreakerSelection(): void {
    this.isSelectionMode = true;
    this.selectedMatches.clear();
  }

  cancelSelection(): void {
    this.isSelectionMode = false;
    this.selectedMatches.clear();
  }

  toggleMatchSelection(matchId: number): void {
    if (!this.isSelectionMode) return;

    // Only allow selection of active matches
    const match = this._matches.find(m => m.id === matchId);
    if (!match?.active) return;

    if (this.selectedMatches.has(matchId)) {
      this.selectedMatches.delete(matchId);
    } else {
      this.selectedMatches.add(matchId);
    }
  }

  isMatchSelected(matchId: number): boolean {
    return this.selectedMatches.has(matchId);
  }

  randomSelectThreeTiebreakers(): void {
    if (!this._matches || this._matches.length === 0) return;

    this.isSelectionMode = true;
    this.selectedMatches.clear();

    // Only consider active matches for random selection
    const activeMatches = this._matches.filter(m => m.active);
    const shuffled = [...activeMatches].sort(() => Math.random() - 0.5);
    const selected = shuffled.slice(0, 3);

    selected.forEach(match => {
      if (match.id != null) {
        this.selectedMatches.add(match.id);
      }
    });
  }

  confirmTiebreakerSelection(): void {
    if (!this.gameweek?.id) {
      alert('Gameweek non définie');
      return;
    }

    if (this.selectedMatches.size < 3) {
      alert('Veuillez sélectionner au moins 3 matchs comme tiebreakers.');
      return;
    }

    const selectedMatchIds = Array.from(this.selectedMatches);

    // Verify all selected matches are still active
    const inactiveSelected = selectedMatchIds.filter(id => {
      const match = this._matches.find(m => m.id === id);
      return !match?.active;
    });

    if (inactiveSelected.length > 0) {
      alert('Certains matchs sélectionnés sont inactifs. Veuillez rafraîchir votre sélection.');
      this.selectedMatches.clear();
      return;
    }

    if (!confirm(`Voulez-vous ajouter ${selectedMatchIds.length} match(es) comme tiebreaker(s) ?`)) {
      return;
    }

    this.gameweekService.setTiebreakers(this.gameweek.id, selectedMatchIds).subscribe({
      next: () => {
        alert(`${selectedMatchIds.length} match(es) tiebreaker ajouté(s) avec succès.`);
        this.isSelectionMode = false;
        this.selectedMatches.clear();

        // Reload fresh data to update UI automatically and emit to parent
        this.reloadGameweekAndMatches();
      },
      error: error => {
        console.error('Erreur lors de l\'ajout des matches tiebreaker :', error);
        alert('Erreur lors de l\'ajout des matches tiebreaker.');
      }
    });
  }

  private reloadGameweekAndMatches(): void {
    if (!this.gameweek?.id) return;

    this.gameweekService.getGameweekById(this.gameweek.id).subscribe({
      next: (updatedGameweek) => {
        this.gameweek = updatedGameweek;
        this.gameweekService.getMatchesWithIcons(this.gameweek.id!).subscribe({
          next: (updatedMatches) => {
            this.matches = updatedMatches;
            // Emit both updated gameweek and matches to parent component
            this.matchesUpdated.emit({ gameweek: updatedGameweek, matches: updatedMatches });
            // This will trigger ngOnChanges and refresh UI
          }
        });
      },
      error: (err) => {
        console.error('Erreur lors du rechargement de la gameweek :', err);
      }
    });
  }
}