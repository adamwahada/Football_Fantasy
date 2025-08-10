import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { GameweekService, Gameweek } from '../../../gameweek.service';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-user-gameweek-details',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './user-gameweek-details.component.html',
  styleUrls: ['./user-gameweek-details.component.scss']
})
export class UserGameweekDetailsComponent implements OnInit {
  competition!: string;
  gameweeks: Gameweek[] = [];
  loading = false;
  error = '';

  constructor(
    private route: ActivatedRoute,
    private gameweekService: GameweekService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      this.competition = params.get('competition') || '';
      if (this.competition) {
        this.loadGameweeks(this.competition);
      }
    });
  }

loadGameweeks(competition: string): void {
  this.loading = true;
  this.error = '';
  this.gameweekService.getUpcomingGameweeks(competition).subscribe({
    next: (data) => {
      // Sort gameweeks by weekNumber ascending
      this.gameweeks = data.sort((a, b) => a.weekNumber - b.weekNumber);
      this.loading = false;
    },
    error: (err) => {
      this.error = 'Erreur lors du chargement des journées.';
      this.loading = false;
      console.error(err);
    }
  });
}


  // Ajoute cette méthode pour formater le nom des compétitions
  formatCompetitionName(enumName: string): string {
    return enumName
      .toLowerCase()
      .split('_')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  }
  closeModal() {
  this.router.navigate(['user/user-gameweek-list']);
}
}
