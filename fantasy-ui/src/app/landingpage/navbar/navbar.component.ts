import { Component, HostListener, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'sportstream-navbar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './navbar.component.html'
})
export class NavbarComponent {
  isMenuOpen = signal(false);
  scrolled = signal(false);

  toggleMenu() {
    this.isMenuOpen.update(v => !v);
  }

  @HostListener('window:scroll')
  onWindowScroll() {
    const y = window.scrollY || 0;
    this.scrolled.set(y > 50);
  }
}