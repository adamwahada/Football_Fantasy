import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { GameweekService, TeamStanding, Gameweek } from '../../../gameweek.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-user-gameweek-classement',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './user-gameweek-classement.component.html',
  styleUrls: ['./user-gameweek-classement.component.scss']
})
export class UserGameweekClassementComponent implements OnInit {
  competition: string | null = null;
  standings: TeamStanding[] = [];
  loading = false;
  error = '';

  constructor(
    private route: ActivatedRoute,
    private gameweekService: GameweekService
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      this.competition = params.get('competition');
      if (this.competition) {
        this.loadLatestClassement(this.competition);
      }
    });
  }

  loadLatestClassement(competition: string): void {
    this.loading = true;
    this.error = '';
    
    this.gameweekService.getAllGameweeksByCompetition(competition).subscribe({
      next: (gameweeks: Gameweek[]) => {
        if (!gameweeks.length) {
          this.error = 'No gameweeks found for this competition.';
          this.loading = false;
          return;
        }

        // Get the latest gameweek (highest weekNumber)
        const latestGameweek = gameweeks.reduce((a, b) => a.weekNumber > b.weekNumber ? a : b);

        // Load league classement for that gameweek
        this.gameweekService.getLeagueClassement(competition, latestGameweek.weekNumber).subscribe({
          next: (standings: TeamStanding[]) => {
            // Ensure standings are sorted correctly by points, goal difference, then goals for
            this.standings = standings.sort((a, b) => 
              b.points - a.points ||
              b.goalsFor - a.goalsFor ||
              (b.goalsFor - b.goalsAgainst) - (a.goalsFor - a.goalsAgainst)
            );
            this.loading = false;
          },
          error: () => {
            this.error = 'Failed to load league table.';
            this.loading = false;
          }
        });
      },
      error: () => {
        this.error = 'Failed to load gameweeks.';
        this.loading = false;
      }
    });
  }
}
