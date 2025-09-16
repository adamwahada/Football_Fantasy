import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { GameweekService, Gameweek } from '../../../gameweek.service';
import { Match } from '../../../../match/match.service';
import { TeamService } from '../../../../match/team.service';
import { forkJoin } from 'rxjs';
import { Location } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';

@Component({
  selector: 'app-user-gameweek-current',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatButtonModule, MatTooltipModule],
  templateUrl: './user-gameweek-current.component.html',
  styleUrls: ['./user-gameweek-current.component.scss']
})
export class UserGameweekCurrentComponent implements OnInit {
  competition!: string;
  weekNumber!: number;
  matches: Match[] = [];
  gameweek: Gameweek | null = null;
  loading = true;
  error = '';
  leagueIconUrl: string = '';
  leagueDisplayName: string = '';
  teamIconsMap: Record<string, string> = {};
  teamsWithIcons: Array<{name: string, iconUrl: string, league: string}> = [];
  leagueIcons: Record<string, string> = {};

  private readonly leagueConfigs: { [key: string]: { displayName: string; iconKey: string } } = {
    'PREMIER_LEAGUE': { displayName: 'Premier League', iconKey: 'Premier League' },
    'LA_LIGA': { displayName: 'La Liga', iconKey: 'La Liga' },
    'SERIE_A': { displayName: 'Serie A', iconKey: 'Serie A' },
    'BUNDESLIGA': { displayName: 'Bundesliga', iconKey: 'Bundesliga' },
    'LIGUE_ONE': { displayName: 'Ligue 1', iconKey: 'Ligue 1' },
    'CHAMPIONS_LEAGUE': { displayName: 'Champions League', iconKey: 'Champions League' },
    'EUROPA_LEAGUE': { displayName: 'Europa League', iconKey: 'Europa League' }
  };

  constructor(
    private route: ActivatedRoute,
    private gameweekService: GameweekService,
    private teamService: TeamService,
    private location: Location
  ) {}

  ngOnInit(): void {
    this.competition = this.route.snapshot.params['competition'];
    const weekParam = this.route.snapshot.params['weekNumber'];
    
    if (weekParam) {
      // If weekNumber is provided in URL, use it
      this.weekNumber = +weekParam;
      console.log('Using weekNumber from URL:', this.weekNumber);
      this.loadIconsAndMatches();
    } else {
      // If no weekNumber provided, determine current gameweek
      console.log('No weekNumber in URL, determining current gameweek');
      this.determineCurrentGameweek();
    }
  }

  private determineCurrentGameweek(): void {
    this.gameweekService.getAllGameweeksByCompetition(this.competition)
      .subscribe({
        next: (gameweeks) => {
          const currentWeek = this.findCurrentGameweek(gameweeks);
          if (currentWeek) {
            this.weekNumber = currentWeek;
            console.log('Current gameweek determined:', this.weekNumber);
            this.loadIconsAndMatches();
          } else {
            this.error = 'No current gameweek found for this competition';
            this.loading = false;
          }
        },
        error: (err) => {
          console.error('Error determining current gameweek:', err);
          this.error = 'Failed to determine current gameweek';
          this.loading = false;
        }
      });
  }

  private findCurrentGameweek(gameweeks: Gameweek[]): number | null {
    const now = new Date();
    const sortedGameweeks = gameweeks.sort((a, b) => a.weekNumber - b.weekNumber);

    console.log('Finding current gameweek from:', sortedGameweeks.length, 'gameweeks');
    console.log('Current time:', now.toISOString());

    // First, try to find a gameweek that is currently ongoing
    const ongoingGameweek = sortedGameweeks.find(gw => {
      const start = new Date(gw.startDate);
      const end = new Date(gw.endDate);
      const isOngoing = now >= start && now <= end;
      
      console.log(`GW${gw.weekNumber}: ${start.toISOString()} - ${end.toISOString()}, ongoing: ${isOngoing}`);
      
      return isOngoing;
    });

    if (ongoingGameweek) {
      console.log('Found ongoing gameweek:', ongoingGameweek.weekNumber);
      return ongoingGameweek.weekNumber;
    }

    // If no ongoing gameweek, find the next upcoming one
    const nextGameweek = sortedGameweeks.find(gw => {
      const start = new Date(gw.startDate);
      return now < start;
    });

    if (nextGameweek) {
      console.log('Found next upcoming gameweek:', nextGameweek.weekNumber);
      return nextGameweek.weekNumber;
    }

    // If no upcoming gameweek, return the last finished one
    const lastGameweek = sortedGameweeks[sortedGameweeks.length - 1];
    console.log('Using last gameweek:', lastGameweek?.weekNumber);
    return lastGameweek?.weekNumber || null;
  }

  private loadIconsAndMatches(): void {
    forkJoin({
      leagueIcons: this.teamService.getAllLeagueIcons(),
      teamIcons: this.teamService.getAllTeamIcons()
    }).subscribe({
      next: ({ leagueIcons, teamIcons }) => {
        this.leagueIcons = leagueIcons || {};
        this.teamIconsMap = teamIcons || {};
        this.setupLeagueInfo();
        this.loadMatches();
        this.loadGameweekInfo();
      },
      error: (err) => {
        console.error('Error loading icons:', err);
        this.setupLeagueInfo();
        this.loadMatches();
        this.loadGameweekInfo();
      }
    });
  }

  private setupLeagueInfo(): void {
    const config = this.leagueConfigs[this.competition];
    if (config) {
      this.leagueDisplayName = config.displayName;
      const iconPath = this.leagueIcons[config.iconKey];
      this.leagueIconUrl = iconPath 
        ? this.teamService.getLeagueIconUrl(iconPath)
        : this.teamService.getLeagueIconUrl('/assets/images/leagues/default.png');
    } else {
      this.leagueDisplayName = this.competition.replace(/_/g, ' ');
      this.leagueIconUrl = this.teamService.getLeagueIconUrl('/assets/images/leagues/default.png');
    }
  }

  private loadMatches(): void {
    console.log('Loading matches for:', this.competition, this.weekNumber);
    
    this.gameweekService.getMatchesByCompetition(this.competition, this.weekNumber)
      .subscribe({
        next: (matches) => {
          console.log('Matches loaded:', matches);
          this.matches = matches;
          this.processTeamIcons();
          this.loading = false;
        },
        error: (err) => {
          console.error('Error loading matches:', err);
          this.error = 'Failed to load matches.';
          this.loading = false;
        }
      });
  }

  private processTeamIcons(): void {
    const allTeams = Array.from(new Set(this.matches.flatMap(m => [m.homeTeam, m.awayTeam])));
    this.teamsWithIcons = this.teamService.getTeamsWithIcons(allTeams, this.teamIconsMap);
    console.log('Teams with icons processed:', this.teamsWithIcons);
  }

  private loadGameweekInfo(): void {
    this.gameweekService.getAllGameweeksByCompetition(this.competition)
      .subscribe({
        next: (gameweeks) => {
          this.gameweek = gameweeks.find(gw => gw.weekNumber === this.weekNumber) || null;
          console.log('Gameweek info loaded:', this.gameweek);
        },
        error: (err) => {
          console.error('Error loading gameweek info:', err);
        }
      });
  }

  getMatchResult(match: Match): string {
    if (typeof match.homeScore !== 'number' || typeof match.awayScore !== 'number') {
      return 'Pas encore joué';
    }

    if (!match.finished) {
      return `${match.homeScore} - ${match.awayScore}`;
    }

    if (match.homeScore > match.awayScore) {
      return match.homeTeam;
    } else if (match.awayScore > match.homeScore) {
      return match.awayTeam;
    } else {
      return 'Égalité';
    }
  }

  isWinner(match: Match, team: 'home' | 'away'): boolean {
    if (!match.finished || typeof match.homeScore !== 'number' || typeof match.awayScore !== 'number') {
      return false;
    }
    if (team === 'home') {
      return match.homeScore > match.awayScore;
    } else {
      return match.awayScore > match.homeScore;
    }
  }

  isLoser(match: Match, team: 'home' | 'away'): boolean {
    if (!match.finished || typeof match.homeScore !== 'number' || typeof match.awayScore !== 'number') {
      return false;
    }
    if (team === 'home') {
      return match.homeScore < match.awayScore;
    } else {
      return match.awayScore < match.homeScore;
    }
  }

  isDraw(match: Match): boolean {
    if (!match.finished || typeof match.homeScore !== 'number' || typeof match.awayScore !== 'number') {
      return false;
    }
    return match.homeScore === match.awayScore;
  }

  getGameweekStatus(): string {
    if (!this.gameweek) return 'En cours';
    
    const now = new Date();
    const start = new Date(this.gameweek.startDate);
    const end = new Date(this.gameweek.endDate);
    
    if (now < start) {
      return 'À venir';
    } else if (now >= start && now <= end) {
      return 'En cours';
    } else {
      return 'Terminé';
    }
  }

  isTiebreakerMatch(match: Match): boolean {
    if (!this.gameweek || !this.gameweek.tiebreakerMatchIds) {
      return false;
    }
    const tiebreakerIds = this.gameweek.tiebreakerMatchIds.split(',').map(id => id.trim());
    return tiebreakerIds.includes(match.id?.toString() || '');
  }

  getTeamIcon(teamName: string): string | undefined {
    const teamWithIcon = this.teamsWithIcons.find(team => team.name === teamName);
    return teamWithIcon?.iconUrl;
  }

  hasTeamIcon(teamName: string): boolean {
    const teamWithIcon = this.teamsWithIcons.find(team => team.name === teamName);
    return !!teamWithIcon?.iconUrl && !teamWithIcon.iconUrl.includes('default.png');
  }

  goBack(): void {
    this.location.back();
  }
}