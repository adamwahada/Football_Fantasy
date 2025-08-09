import { Component, Input, Output, EventEmitter, OnChanges, SimpleChanges } from '@angular/core';
import { MatchWithIconsDTO } from '../../../match/match.service';
import { Gameweek, GameweekService } from '../../gameweek.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-show-gameweek-matches',
  templateUrl: './show-gameweek-matches.component.html',
  styleUrls: ['./show-gameweek-matches.component.scss'],
  standalone: true,
  imports: [CommonModule, CommonModule]
})
export class ShowGameweekMatchesComponent implements OnChanges {
  @Input() gameweek!: Gameweek;
  @Input() matches: MatchWithIconsDTO[] = [];

  private _matches: (MatchWithIconsDTO & { isTiebreaker?: boolean })[] = [];

  // Nouvelles propriétés pour la sélection
  isSelectionMode = false;
  selectedMatches: Set<number> = new Set();

  @Output() close = new EventEmitter<void>();

  readonly baseUrl = 'http://localhost:9090/fantasy';

  constructor(private gameweekService: GameweekService) {}

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
      isTiebreaker: m.id != null && tiebreakerIds.includes(m.id)
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

    this.gameweekService.deleteSelectedMatches(this.gameweek.id, [matchId]).subscribe({
      next: () => {
        this._matches = this._matches.filter(m => m.id !== matchId);
      },
      error: err => {
        console.error('Erreur lors de la suppression du match :', err);
        alert('Erreur lors de la suppression du match.');
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

    const shuffled = [...this._matches].sort(() => Math.random() - 0.5);
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

    if (!confirm(`Voulez-vous ajouter ${selectedMatchIds.length} match(es) comme tiebreaker(s) ?`)) {
      return;
    }

    this.gameweekService.setTiebreakers(this.gameweek.id, selectedMatchIds).subscribe({
      next: () => {
        alert(`${selectedMatchIds.length} match(es) tiebreaker ajouté(s) avec succès.`);
        this.isSelectionMode = false;
        this.selectedMatches.clear();
      },
      error: error => {
        console.error('Erreur lors de l\'ajout des matches tiebreaker :', error);
        alert('Erreur lors de l\'ajout des matches tiebreaker.');
      }
    });
  }
}
