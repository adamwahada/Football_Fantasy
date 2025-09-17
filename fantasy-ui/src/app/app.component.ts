import { Component } from '@angular/core';
import { RouterModule, Router, Event, NavigationStart, NavigationEnd, RouterOutlet } from '@angular/router';
import { AuthService } from './core/services/auth.service';
import { CommonModule } from '@angular/common';
import { TranslateService, TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, TranslateModule],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss']
})
export class AppComponent {
  currentUrl!: string;

  constructor(
    public _router: Router,
    public auth: AuthService,
    private translate: TranslateService
  ) {
    // Router event tracking
    this._router.events.subscribe((routerEvent: Event) => {
      if (routerEvent instanceof NavigationStart) {
        this.currentUrl = routerEvent.url.substring(
          routerEvent.url.lastIndexOf('/') + 1
        );
      }
      if (routerEvent instanceof NavigationEnd) {
        // navigation terminée
      }
      window.scrollTo(0, 0); // scroll en haut après chaque navigation
    });

    // 🔹 Setup translations
    this.translate.addLangs(['en', 'fr', 'ar']);   // available languages
    this.translate.setDefaultLang('en');           // default language

    // if user has a saved language, use it
    const savedLang = localStorage.getItem('lang');
    this.translate.use(savedLang ? savedLang : 'en');
  }

  switchLang(lang: string) {
    this.translate.use(lang);
    localStorage.setItem('lang', lang); // persist choice
  }

  login() {
    this.auth.login();
  }

  logout(): void {
    this.auth.logout();
  }

  register(): void {
    // Implémente l'inscription ici
  }

  ngOnInit() {
    if (this.auth.isLoggedIn()) {
      this.auth.loadCurrentUser().subscribe(user => {
        console.log('User loaded:', user);
      });
    }
  }
}
