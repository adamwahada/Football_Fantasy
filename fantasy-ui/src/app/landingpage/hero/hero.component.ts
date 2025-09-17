import { Component, HostListener, OnDestroy, OnInit, signal } from '@angular/core'
import { CommonModule } from '@angular/common'

@Component({
  selector: 'sportstream-hero',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="relative h-screen flex items-center justify-center overflow-hidden">
      <ng-container *ngFor="let image of backgroundImages; let index = index">
        <div
          class="absolute inset-0 transition-all duration-1000"
          [class.opacity-100]="index === currentImageIndex()"
          [class.opacity-0]="index !== currentImageIndex()"
          [style.opacity]="index === currentImageIndex() ? backgroundOpacity() : 0"
        >
          <img [src]="image" [alt]="'Sports background ' + (index + 1)" class="w-full h-full object-cover" />
          <div class="absolute inset-0 bg-black transition-opacity duration-300" [style.opacity]="overlayOpacity()"></div>
        </div>
      </ng-container>

      <div class="relative z-10 text-center max-w-4xl mx-auto px-4 transition-all duration-300" [style.transform]="'translateY(' + contentTranslateY() + 'px)'"><!-- animate fade-in via class below -->
        <div class="animate-fade-in">
          <button class="app-title-btn">
            <h1 class="text-5xl md:text-7xl font-bold text-white mb-6 text-balance transition-all duration-300">
              Live Sports
              <span class="block text-\[\#0dfd86\]">Streaming</span>
            </h1>
          </button>

          <p class="text-xl md:text-2xl text-white/90 mb-8 max-w-2xl mx-auto text-pretty">
            Experience the thrill of live sports with premium streaming quality. Never miss a moment of the action.
          </p>

          <div class="flex flex-col sm:flex-row gap-4 justify-center items-center">
            <button class="bg-\[\#0dfd86\] hover:bg-\[\#0dfd86\]\/90 text-\[\#0a0f0a\] px-8 py-4 text-lg font-semibold rounded flex items-center">
              <span class="mr-2">▶</span>
              Watch Live Now
            </button>
            <button class="border border-white text-white hover:bg-white hover:text-\[\#0a0f0a\] px-8 py-4 text-lg bg-transparent rounded flex items-center">
              View Schedule
              <span class="ml-2">›</span>
            </button>
          </div>

          <div class="grid grid-cols-3 gap-8 mt-16 max-w-2xl mx-auto">
            <div class="text-center">
              <div class="text-3xl md:text-4xl font-bold text-\[\#0dfd86\]">50+</div>
              <div class="text-white/80 text-sm md:text-base">Sports Channels</div>
            </div>
            <div class="text-center">
              <div class="text-3xl md:text-4xl font-bold text-\[\#0dfd86\]">24/7</div>
              <div class="text-white/80 text-sm md:text-base">Live Coverage</div>
            </div>
            <div class="text-center">
              <div class="text-3xl md:text-4xl font-bold text-\[\#0dfd86\]">4K</div>
              <div class="text-white/80 text-sm md:text-base">Ultra HD Quality</div>
            </div>
          </div>
        </div>
      </div>

      <div class="absolute bottom-8 left-1/2 transform -translate-x-1/2 animate-bounce">
        <div class="w-6 h-10 border-2 border-white/50 rounded-full flex justify-center">
          <div class="w-1 h-3 bg-white/50 rounded-full mt-2 animate-pulse"></div>
        </div>
      </div>
    </section>
  `,
})
export class HeroComponent implements OnInit, OnDestroy {
backgroundImages = [
  'assets/images/placeholder-vo7ga.png',
  'assets/images/placeholder-5zk5l.png',
  'assets/images/basketball-player-dunking-in-arena.jpg',
  'assets/images/placeholder-jo717.png',
  'assets/images/placeholder-ep3e9.png',
]

  currentImageIndex = signal(0)
  scrollY = signal(0)
  private intervalId: any

  ngOnInit(): void {
    this.intervalId = setInterval(() => {
      const next = (this.currentImageIndex() + 1) % this.backgroundImages.length
      this.currentImageIndex.set(next)
    }, 5000)
  }

  ngOnDestroy(): void {
    if (this.intervalId) {
      clearInterval(this.intervalId)
    }
  }

  @HostListener('window:scroll')
  onWindowScroll() {
    this.scrollY.set(window.scrollY || 0)
  }

  backgroundOpacity() {
    const s = this.scrollY()
    return Math.max(0.2, 1 - s / 1000)
  }

  overlayOpacity() {
    const s = this.scrollY()
    return 0.1 + (s / 1200) * 0.4
  }

  contentTranslateY() {
    return this.scrollY() * 0.3
  }
}
