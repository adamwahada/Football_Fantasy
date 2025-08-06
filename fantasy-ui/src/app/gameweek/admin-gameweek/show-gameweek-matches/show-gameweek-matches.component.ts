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
}