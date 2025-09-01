import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { GameweekService, TeamStanding, Gameweek } from '../../../gameweek.service';
import { TeamService, TeamIcon } from '../../../../match/team.service';
import { CommonModule } from '@angular/common'; 
import { Location } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-user-gameweek-classement',
  standalone: true,
  imports: [CommonModule,    MatIconModule,  MatButtonModule
],
  templateUrl: './user-gameweek-classement.component.html',
  styleUrls: ['./user-gameweek-classement.component.scss']
})
export class UserGameweekClassementComponent implements OnInit {
  competition: string | null = null;
  standings: TeamStanding[] = [];
  loading = false;
  error = '';

  leagueIcons: Record<string, string> = {};
  teamIconsMap: Record<string, string> = {};
  teamsWithIcons: TeamIcon[] = [];
  leagueIconUrl: string = '';

  private leagueConfigs: Record<string, { displayName: string; iconKey: string; theme: string }> = {
    'PREMIER_LEAGUE': { displayName: 'Premier League', iconKey: 'Premier League', theme: 'premier-league' },
    'LA_LIGA': { displayName: 'La Liga', iconKey: 'La Liga', theme: 'la-liga' },
    'SERIE_A': { displayName: 'Serie A', iconKey: 'Serie A', theme: 'serie-a' },
    'BUNDESLIGA': { displayName: 'Bundesliga', iconKey: 'Bundesliga', theme: 'bundesliga' },
    'LIGUE_ONE': { displayName: 'Ligue 1', iconKey: 'Ligue 1', theme: 'ligue-1' },
    'CHAMPIONS_LEAGUE': { displayName: 'Champions League', iconKey: 'Champions League', theme: 'champions-league' },
    'EUROPA_LEAGUE': { displayName: 'Europa League', iconKey: 'Europa League', theme: 'europa-league' },
    'CONFERENCE_LEAGUE': { displayName: 'Conference League', iconKey: 'Conference League', theme: 'conference-league' },
    'BESTOFF': { displayName: 'Best Of', iconKey: 'Best Of', theme: 'best-of' }
  };

  constructor(
    private route: ActivatedRoute,
    private gameweekService: GameweekService,
    public teamService: TeamService ,
    private location: Location  ) {}

  ngOnInit(): void {
    // Load all icons first
    Promise.all([
      this.teamService.getAllLeagueIcons().toPromise(),
      this.teamService.getAllTeamIcons().toPromise()
    ]).then(([leagueIcons, teamIcons]) => {
      this.leagueIcons = leagueIcons || {};
      this.teamIconsMap = teamIcons || {};
      this.teamsWithIcons = this.teamService.getTeamsWithIcons(
        Object.keys(this.teamIconsMap),
        this.teamIconsMap
      );

      this.route.paramMap.subscribe(params => {
        this.competition = params.get('competition');
        if (this.competition) {
          this.leagueIconUrl = this.getLeagueIconUrl(this.competition);
          this.loadLatestClassement(this.competition);
        }
      });
    }).catch(() => {
      this.error = 'Failed to load icons.';
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
        const latestGameweek = gameweeks.reduce((a, b) => a.weekNumber > b.weekNumber ? a : b);
        this.gameweekService.getLeagueClassement(competition, latestGameweek.weekNumber).subscribe({
          next: (standings: TeamStanding[]) => {
            // Always map iconUrl for each team, fallback to default if not found
            this.standings = standings.map(team => ({
              ...team,
              iconUrl: this.getTeamIconUrl(team.teamName)
            }))
            // Sort by points, goal difference, goals for
            .sort((a, b) =>
              b.points - a.points ||
              (b.goalsFor - b.goalsAgainst) - (a.goalsFor - a.goalsAgainst) ||
              b.goalsFor - a.goalsFor
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

  getLeagueIconUrl(competition: string): string {
    const config = this.leagueConfigs[competition];
    if (!config || !this.leagueIcons[config.iconKey]) {
      return this.teamService.getLeagueIconUrl('/assets/images/leagues/default.png');
    }
    return this.teamService.getLeagueIconUrl(this.leagueIcons[config.iconKey]);
  }

getTeamIconUrl(teamName: string): string {
  const teamsWithIcons = this.teamService.getTeamsWithIcons([teamName], this.teamIconsMap);
  return teamsWithIcons.length ? teamsWithIcons[0].iconUrl : this.teamService.getDefaultIconPath(teamName);
}


  formatCompetitionName(enumName: string): string {
    const config = this.leagueConfigs[enumName];
    return config?.displayName || enumName
      .toLowerCase()
      .split('_')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  }

getRowClass(position: number): string {
  // The CSS handles position styling automatically via nth-child
  // This method can be used for additional row classes if needed
  return '';
}

getGDClass(goalDifference: number): string {
  if (goalDifference > 0) return 'gd-positive';
  if (goalDifference < 0) return 'gd-negative';
  return 'gd-zero';
}

goBack(): void {
  this.location.back();
}
}
