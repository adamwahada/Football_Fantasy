import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { GameweekService, Gameweek } from '../../../gameweek.service';
import { Match } from '../../../../match/match.service';
import { TeamService } from '../../../../match/team.service';
import { forkJoin } from 'rxjs';

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
  gameweek: Gameweek | null = null;
  loading = true;
  error = '';
  leagueIconUrl: string = '';
  leagueDisplayName: string = '';

  // Pre-loaded icon maps for performance
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
    private teamService: TeamService
  ) {}

  ngOnInit(): void {
    this.competition = this.route.snapshot.params['competition'];
    this.weekNumber = +this.route.snapshot.params['weekNumber'];

    console.log('UserGameweekFinishedComponent initialized with:', {
      competition: this.competition,
      weekNumber: this.weekNumber
    });

    // Load all icons first, then load matches
    this.loadIconsAndMatches();
  }

  private loadIconsAndMatches(): void {
    // Load all icons in parallel first
    forkJoin({
      leagueIcons: this.teamService.getAllLeagueIcons(),
      teamIcons: this.teamService.getAllTeamIcons()
    }).subscribe({
      next: ({ leagueIcons, teamIcons }) => {
        this.leagueIcons = leagueIcons || {};
        this.teamIconsMap = teamIcons || {};
        
        // Setup league info after icons are loaded
        this.setupLeagueInfo();
        
        // Now load matches
        this.loadMatches();
      },
      error: (err) => {
        console.error('Error loading icons:', err);
        // Continue without icons
        this.setupLeagueInfo();
        this.loadMatches();
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

  loadMatches(): void {
    console.log('Loading matches for:', this.competition, this.weekNumber);
    
    this.gameweekService.getMatchesByCompetition(this.competition, this.weekNumber)
      .subscribe({
        next: (data) => {
          console.log('Matches loaded:', data);
          this.matches = data;
          
          // Process team icons after matches are loaded
          this.processTeamIcons();
          
          this.loadGameweekInfo();
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
    // Get all unique team names from matches
    const allTeams = Array.from(new Set(
      this.matches.flatMap(match => [match.homeTeam, match.awayTeam])
    ));
    
    // Use the TeamService method to get teams with proper icon URLs
    this.teamsWithIcons = this.teamService.getTeamsWithIcons(allTeams, this.teamIconsMap);
    
    console.log('Teams with icons processed:', this.teamsWithIcons);
  }

  private loadGameweekInfo(): void {
    this.gameweekService.getAllGameweeksByCompetition(this.competition)
      .subscribe({
        next: (gameweeks) => {
          this.gameweek = gameweeks.find(gw => gw.weekNumber === this.weekNumber) || null;
        },
        error: (err) => {
          console.error('Error loading gameweek info:', err);
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

  isTiebreakerMatch(match: Match): boolean {
    if (!this.gameweek || !this.gameweek.tiebreakerMatchIds) {
      return false;
    }
    const tiebreakerIds = this.gameweek.tiebreakerMatchIds.split(',').map(id => id.trim());
    return tiebreakerIds.includes(match.id?.toString() || '');
  }

  // Optimized method - synchronous lookup from processed teams with icons
  getTeamIcon(teamName: string): string | undefined {
    const teamWithIcon = this.teamsWithIcons.find(team => team.name === teamName);
    return teamWithIcon?.iconUrl;
  }

  // Method to check if team has a custom icon
  hasTeamIcon(teamName: string): boolean {
    const teamWithIcon = this.teamsWithIcons.find(team => team.name === teamName);
    return !!teamWithIcon?.iconUrl && !teamWithIcon.iconUrl.includes('default.png');
  }
}