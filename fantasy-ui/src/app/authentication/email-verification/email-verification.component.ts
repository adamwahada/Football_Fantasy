import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { interval, Subscription } from 'rxjs';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-email-verification',
  templateUrl: './email-verification.component.html',
  styleUrls: ['./email-verification.component.scss']
})
export class EmailVerificationComponent implements OnInit, OnDestroy {
  private pollSub?: Subscription;

  constructor(private router: Router, private authService: AuthService) {}

  ngOnInit() {
    // Poll every 5 seconds to check if email is verified
    this.pollSub = interval(5000).subscribe(() => {
      this.authService.checkEmailVerified().subscribe(isVerified => {
        if (isVerified) {
          this.router.navigate(['/user-dashboard']);
        }
      });
    });
  }

  ngOnDestroy() {
    this.pollSub?.unsubscribe();
  }

  tryLoginAgain() {
    this.router.navigate(['/signin']);
  }
}