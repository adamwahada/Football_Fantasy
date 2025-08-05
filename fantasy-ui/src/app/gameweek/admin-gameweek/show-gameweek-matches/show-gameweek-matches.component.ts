import { Component, Input, Output, EventEmitter } from '@angular/core';
import { Match, MatchWithIconsDTO } from '../../../match/match.service';
import { Gameweek } from '../../gameweek.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-show-gameweek-matches',
  templateUrl: './show-gameweek-matches.component.html',
  styleUrls: ['./show-gameweek-matches.component.scss'],
  standalone: true,
  imports: [CommonModule]
})
export class ShowGameweekMatchesComponent {
  @Input() gameweek!: Gameweek;

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

  getMatchStatus(match: MatchWithIconsDTO): string {
    // Vérifier d'abord si le match est inactif (active = 0)
    if ((match.active as any) === 0) {
      return 'Inactif';
    }
    
    // Vérifier si le match est terminé (finished = 1)
    if ((match.finished as any) === 1) {
      return 'Fini';
    }
    
    // Sinon c'est un match à venir
    return 'À venir';
  }
}