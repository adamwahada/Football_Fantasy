import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MatchService, Match } from '../../match.service'; // adapte le chemin si besoin
import { FormBuilder, FormGroup, Validators } from '@angular/forms';

@Component({
  selector: 'app-edit-admin-match',
  templateUrl: './edit-admin-match.component.html',
  styleUrls: ['./edit-admin-match.component.scss'],
  standalone: true,
  imports: [],
})
export class EditAdminMatchComponent implements OnInit {
  matchForm: FormGroup;
  matchId!: number;
  match?: Match;

  constructor(
    private route: ActivatedRoute,
    private matchService: MatchService,
    private fb: FormBuilder
  ) {
    this.matchForm = this.fb.group({
      homeTeam: ['', Validators.required],
      awayTeam: ['', Validators.required],
      matchDate: ['', Validators.required],
      matchTime: ['', Validators.required],
      homeScore: [null],
      awayScore: [null],
      description: [''],
      status: ['SCHEDULED', Validators.required],
      active: [true],
    });
  }

  ngOnInit() {
    this.matchId = +this.route.snapshot.paramMap.get('id')!;
    this.matchService.getMatchById(this.matchId).subscribe(match => {
      this.match = match;
      // Sépare la date et l'heure pour le formulaire
      const d = new Date(match.matchDate);
      this.matchForm.patchValue({
        ...match,
        matchDate: d.toISOString().slice(0, 10),
        matchTime: d.toTimeString().slice(0, 5),
      });
    });
  }

}