import { NavbarComponent } from './navbar/navbar.component';
import { HeroComponent } from './hero/hero.component';
import { FeaturesComponent } from './features/features.component';
import { TrendingComponent } from './trending/trending.component';
import { FooterComponent } from './footer/footer.component';
import { Component } from '@angular/core';

@Component({
  selector: 'app-landingpage',
  standalone: true,
  imports: [NavbarComponent, HeroComponent, FeaturesComponent, TrendingComponent, FooterComponent],
  templateUrl: './landingpage.component.html'
})
export class LandingpageComponent {}
