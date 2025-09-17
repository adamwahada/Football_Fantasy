import { Component, ElementRef, OnInit, ViewChild } from '@angular/core'
import { CommonModule } from '@angular/common'


interface TrendItem {
  id: number
  title: string
  sport: string
  time: string
  image: string
  isLive: boolean
}

@Component({
  selector: 'sportstream-trending',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="py-16 bg-background">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="text-center mb-12">
          <h2 class="text-3xl md:text-4xl font-bold text-foreground mb-4">Trending Now</h2>
          <p class="text-lg text-foreground/80 max-w-2xl mx-auto">
            Catch up on the hottest sports action and live events happening right now
          </p>
        </div>

        <div class="overflow-hidden">
          <div #scrollRef class="flex gap-6 scroll-container animate-scroll" style="width: max-content;">
            <ng-container *ngFor="let item of duplicated(); let index = index">
              <div class="flex-shrink-0 w-80 trending-card bg-card/10 border border-\[\#0dfd86\]\/20 transition-all duration-400 cursor-pointer group hover:border-\[\#0dfd86\]\/60 hover:bg-card/20 hover:shadow-lg hover:shadow-\[\#0dfd86\]\/20 rounded">
                <div class="p-0 relative z-10">
                  <div class="relative overflow-hidden rounded-t-lg">
                    <img [src]="item.image" [alt]="item.title" class="w-full h-48 object-cover rounded-t-lg transition-transform duration-400 group-hover:scale-110" />
                    <div class="absolute inset-0 bg-black/40 rounded-t-lg opacity-0 group-hover:opacity-100 transition-opacity duration-300 flex items-center justify-center">
                      <span class="w-12 h-12 text-white transform scale-75 group-hover:scale-100 transition-transform duration-300 inline-block">▶</span>
                    </div>
                    <span *ngIf="item.isLive" class="absolute top-3 left-3 bg-red-600 text-white px-2 py-1 rounded text-xs animate-pulse">LIVE</span>
                  </div>
                  <div class="p-4">
                    <h3 class="font-semibold text-foreground mb-2 text-lg group-hover:text-\[\#0dfd86\] transition-colors duration-300">{{ item.title }}</h3>
                    <div class="flex items-center justify-between text-sm text-foreground/70">
                      <span class="bg-\[\#0dfd86\]\/20 text-\[\#0dfd86\] px-2 py-1 rounded-full group-hover:bg-\[\#0dfd86\]\/30 transition-colors duration-300">{{ item.sport }}</span>
                      <div class="flex items-center gap-1">
                        <span class="w-4 h-4 inline-block">⏰</span>
                        {{ item.time }}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </ng-container>
          </div>
        </div>
      </div>
    </section>
  `,
})
export class TrendingComponent implements OnInit {
  @ViewChild('scrollRef', { read: ElementRef }) scrollRef!: ElementRef<HTMLDivElement>

  items: TrendItem[] = [
    { id: 1, title: 'Champions League Final', sport: 'Football', time: 'Live Now', image: '/champions-league-football-match.jpg', isLive: true },
    { id: 2, title: 'NBA Finals Game 7', sport: 'Basketball', time: '2 hours ago', image: '/nba-basketball-finals-game.jpg', isLive: false },
    { id: 3, title: 'Wimbledon Semi-Final', sport: 'Tennis', time: 'Live Now', image: '/wimbledon-tennis-match.jpg', isLive: true },
    { id: 4, title: 'World Cup Qualifier', sport: 'Football', time: '30 min ago', image: '/world-cup-football-qualifier.jpg', isLive: false },
    { id: 5, title: 'Boxing Championship', sport: 'Boxing', time: 'Live Now', image: '/boxing-championship-fight.jpg', isLive: true },
    { id: 6, title: 'Formula 1 Grand Prix', sport: 'Racing', time: '1 hour ago', image: '/formula-1-racing-grand-prix.jpg', isLive: false },
  ]

  ngOnInit(): void {}

  duplicated() {
    return [...this.items, ...this.items]
  }
}
