import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { GameweekService } from '../../../gameweek.service';
import { Match } from '../../../../match/match.service';

@Component({
  selector: 'app-user-gameweek-finished',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './user-gameweek-finished.component.html',
  styleUrls: ['./user-gameweek-finished.component.scss']
})
export class UserGameweekFinishedComponent implements OnInit {
  competition!: string;
  weekNumber!: number;
  matches: Match[] = [];
  loading = true;
  error = '';

  constructor(
    private route: ActivatedRoute,
    private gameweekService: GameweekService
  ) {}

  ngOnInit(): void {
    this.competition = this.route.snapshot.params['competition'];
    this.weekNumber = +this.route.snapshot.params['weekNumber'];

    console.log('UserGameweekFinishedComponent initialized with:', {
      competition: this.competition,
      weekNumber: this.weekNumber
    });

    this.loadMatches();
  }

  loadMatches(): void {
    console.log('Loading matches for:', this.competition, this.weekNumber);
    
    this.gameweekService.getMatchesByCompetition(this.competition, this.weekNumber)
      .subscribe({
        next: (data) => {
          console.log('Matches loaded:', data);
          this.matches = data;
          this.loading = false;
        },
        error: (err) => {
          console.error('Error loading matches:', err);
          this.error = 'Failed to load matches.';
          this.loading = false;
        }
      });
  }

  getWinner(match: Match): string {
    if (!match.finished) return 'Pas encore joué';
    if (typeof match.homeScore !== 'number' || typeof match.awayScore !== 'number') return 'Score inconnu';
    if (match.homeScore > match.awayScore) return match.homeTeam;
    if (match.awayScore > match.homeScore) return match.awayTeam;
    return 'Égalité';
  }
}
