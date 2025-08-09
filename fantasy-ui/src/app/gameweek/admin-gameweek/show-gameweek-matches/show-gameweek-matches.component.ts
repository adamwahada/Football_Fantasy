import { Component, Input, Output, EventEmitter } from '@angular/core';
import { Match, MatchWithIconsDTO } from '../../../match/match.service';
import { Gameweek,GameweekService } from '../../gameweek.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-show-gameweek-matches',
  templateUrl: './show-gameweek-matches.component.html',
  styleUrls: ['./show-gameweek-matches.component.scss'],
  standalone: true,
  imports: [CommonModule, CommonModule]
})

export class ShowGameweekMatchesComponent {
  @Input() gameweek!: Gameweek;
  constructor(private gameweekService: GameweekService) {}
  private _matches: MatchWithIconsDTO[] = [];

  // Nouvelles propriétés pour la sélection
  isSelectionMode = false;
  selectedMatches: Set<number> = new Set();

  @Input() set matches(value: MatchWithIconsDTO[]) {
    // Ne rien modifier, juste assigner directement
    this._matches = value;
  }

  get matches(): MatchWithIconsDTO[] {
    return this._matches;
  }

  @Output() close = new EventEmitter<void>();

  readonly baseUrl = 'http://localhost:9090/fantasy';

  formatCompetition(comp?: string): string {
    return comp?.replace(/_/g, ' ') || '';
  }

  getFullIconUrl(relativePath: string): string {
    if (!relativePath) return `${this.baseUrl}/assets/images/teams/default.png`;
    if (relativePath.startsWith('http')) return relativePath;
    return `${this.baseUrl}${relativePath}`;
  }

  getMatchStatus(match: any): string {
    if (!match.active || match.active === false) {
      return 'Inactif';
    }
    
    if (match.finished) {
      return 'Fini';
    }
    
    switch (match.status) {
      case 'LIVE':
        return 'En cours';
      case 'COMPLETED':
        return 'Fini';
      case 'CANCELED':
        return 'Annulé';
      case 'SCHEDULED':
      default:
        return 'À venir';
    }
  }

  removeMatch(matchId: number): void {
    if (!this.gameweek?.id) return;

    const confirmDelete = confirm('Voulez-vous vraiment retirer ce match de cette gameweek ?');
    if (!confirmDelete) return;

    this.gameweekService.deleteSelectedMatches(this.gameweek.id, [matchId]).subscribe({
      next: () => {
        this._matches = this._matches.filter(m => m.id !== matchId);
      },
      error: (err) => {
        console.error('Erreur lors de la suppression du match :', err);
        alert('Erreur lors de la suppression du match.');
      }
    });
  }

  // Nouvelle méthode pour activer le mode sélection
  startTiebreakerSelection(): void {
    this.isSelectionMode = true;
    this.selectedMatches.clear();
  }

  // Méthode pour annuler la sélection
  cancelSelection(): void {
    this.isSelectionMode = false;
    this.selectedMatches.clear();
  }

  // Méthode pour gérer la sélection d'un match
  toggleMatchSelection(matchId: number): void {
    if (!this.isSelectionMode) return;

    if (this.selectedMatches.has(matchId)) {
      this.selectedMatches.delete(matchId);
    } else {
      this.selectedMatches.add(matchId);
    }
  }

  // Méthode pour vérifier si un match est sélectionné
  isMatchSelected(matchId: number): boolean {
    return this.selectedMatches.has(matchId);
  }

  // Méthode pour confirmer la sélection et ajouter les tiebreakers
  confirmTiebreakerSelection(): void {
    if (!this.gameweek?.id) {
      alert('Gameweek non définie');
      return;
    }

    if (this.selectedMatches.size === 0) {
      alert('Veuillez sélectionner au moins un match.');
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
      error: (error) => {
        console.error('Erreur lors de l\'ajout des matches tiebreaker :', error);
        alert('Erreur lors de l\'ajout des matches tiebreaker.');
      }
    });
  }
randomSelectThreeTiebreakers(): void {
  if (!this.matches || this.matches.length === 0) return;

  this.isSelectionMode = true; // enable selection mode to highlight selections
  this.selectedMatches.clear();

  const shuffled = [...this.matches].sort(() => Math.random() - 0.5);
  const selected = shuffled.slice(0, 3);

  selected.forEach(match => {
    if (match.id != null) {
      this.selectedMatches.add(match.id);
    }
  });
}


}