import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div style="max-width: 900px; margin: 2rem auto; padding: 1.5rem;">
      <h1 style="margin-bottom: 0.5rem;">Welcome to Football Fantasy</h1>
      <p style="color:#555; margin-top: 0;">Your squad, your tactics, your glory.</p>

      <section style="margin-top: 1.5rem;">
        <h2>How it works</h2>
        <ol style="line-height: 1.6;">
          <li>Create your squad within the budget cap.</li>
          <li>Pick your starting XI and set your captain.</li>
          <li>Earn points each matchday based on real player performances.</li>
          <li>Transfers: manage your team between rounds to optimize points.</li>
          <li>Compete in leagues with friends or climb the global leaderboard.</li>
        </ol>
      </section>

      <section style="margin-top: 1rem;">
        <h2>Rules at a glance</h2>
        <ul style="line-height: 1.6;">
          <li>Budget: Stay under the salary cap when building your squad.</li>
          <li>Formations: Standard formations (e.g., 4-4-2, 4-3-3, 3-5-2).</li>
          <li>Captain: Scores double points for that round.</li>
          <li>Transfers: Limited free transfers per round; extra transfers cost points.</li>
          <li>Deadlines: Changes lock before the first match of each round.</li>
        </ul>
      </section>

      <section style="margin-top: 1rem;">
        <h2>Scoring basics</h2>
        <ul style="line-height: 1.6;">
          <li>Minutes played, goals, assists add points.</li>
          <li>Clean sheets and saves reward defenders and goalkeepers.</li>
          <li>Cards, own goals, and missed penalties deduct points.</li>
        </ul>
      </section>

      <div style="margin-top: 2rem;">
        <button (click)="proceed()" style="padding: 0.6rem 1rem;">Got it, take me to my dashboard</button>
      </div>
    </div>
  `
})
export class LandingComponent {
  constructor(private router: Router) {}

  proceed() {
    localStorage.setItem('landingSeen', '1');
    this.router.navigateByUrl('/dashboard');
  }
}