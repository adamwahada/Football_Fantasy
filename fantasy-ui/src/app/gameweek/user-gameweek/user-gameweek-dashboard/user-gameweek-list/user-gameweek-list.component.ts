import { Component, OnInit } from '@angular/core';
import { TeamService } from '../../../../match/team.service';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

interface League {
  displayName: string;
  iconUrl: string;
  competitionCode: string;
}

@Component({
  selector: 'app-user-gameweek-list',
  templateUrl: './user-gameweek-list.component.html',
  styleUrls: ['./user-gameweek-list.component.scss'],
  imports: [CommonModule], 
})
export class UserGameweekListComponent implements OnInit {
  leagues: { value: string; label: string; iconUrl: string }[] = [];
  loading = true;
  error = '';

  competitions = [
    { value: 'PREMIER_LEAGUE', label: 'Premier League' },
    { value: 'LA_LIGA', label: 'La Liga' },
    { value: 'SERIE_A', label: 'Serie A' },
    { value: 'BUNDESLIGA', label: 'Bundesliga' },
    { value: 'LIGUE_ONE', label: 'Ligue 1' },
    { value: 'CHAMPIONS_LEAGUE', label: 'Champions League' },
    { value: 'EUROPE_LEAGUE', label: 'Europa League' },
  ];

  constructor(private teamService: TeamService, private router: Router) {}

  ngOnInit(): void {
    this.loadLeagues();
  }

  loadLeagues(): void {
    this.loading = true;
    this.teamService.getAllLeagueIcons().subscribe({
      next: (leagueIconsMap) => {
        this.leagues = this.competitions.map(league => ({
          value: league.value,
          label: league.label,
          iconUrl: leagueIconsMap[league.label]
            ? this.teamService['convertToFullUrl'](leagueIconsMap[league.label])
            : '/assets/images/leagues/default.png',
        }));
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load leagues';
        this.loading = false;
        console.error(err);
      },
    });
  }

  onLeagueClick(competitionCode: string): void {
    this.router.navigate(['user/user-gameweek-list', competitionCode]);
  }
}
