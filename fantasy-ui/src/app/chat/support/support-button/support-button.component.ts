import { Component, Output, EventEmitter } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-support-button',
  templateUrl: './support-button.component.html',
  styleUrls: ['./support-button.component.scss'],
  standalone: false
})
export class SupportButtonComponent {
  @Output() supportClicked = new EventEmitter<void>();

  constructor(private router: Router) {}

  onSupportClick(): void {
    this.supportClicked.emit();
    this.router.navigate(['/support']);
  }
}
