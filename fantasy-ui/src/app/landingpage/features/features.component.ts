import { Component, ElementRef, OnDestroy, OnInit, QueryList, ViewChildren, signal } from '@angular/core'
import { CommonModule } from '@angular/common'

interface FeatureItem {
  icon: string
  title: string
  description: string
}

@Component({
  selector: 'sportstream-features',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="py-16 bg-background gameweek-list-container">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
        <div class="text-center mb-12">
          <h2 class="text-3xl md:text-4xl font-bold text-foreground mb-4">Why Choose SportStream?</h2>
          <p class="text-lg text-foreground/80 max-w-2xl mx-auto">
            Experience sports like never before with our cutting-edge streaming platform
          </p>
        </div>

        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
          <div
            *ngFor="let feature of features; let i = index"
            #featureEl
            class="transform transition-all duration-700"
            [class.translate-y-0]="isVisible(i)"
            [class.opacity-100]="isVisible(i)"
            [class.translate-y-8]="!isVisible(i)"
            [class.opacity-0]="!isVisible(i)"
            [style.transitionDelay]="(i * 100) + 'ms'"
          >
            <div class="h-full feature-card bg-card border border-border/20 transition-all duration-400 group rounded">
              <div class="p-6 text-center relative z-10">
                <div class="w-16 h-16 bg-primary/10 rounded-full flex items-center justify-center mx-auto mb-4 group-hover:bg-primary/20 transition-all duration-300 group-hover:shadow-lg">
                  <span class="w-8 h-8 text-primary group-hover:scale-110 transition-transform duration-300">{{ feature.icon }}</span>
                </div>
                <h3 class="text-xl font-semibold text-card-foreground mb-3 group-hover:text-primary transition-colors duration-300">{{ feature.title }}</h3>
                <p class="text-card-foreground/80 leading-relaxed">{{ feature.description }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  `,
})
export class FeaturesComponent implements OnInit, OnDestroy {
  @ViewChildren('featureEl', { read: ElementRef }) featureElements!: QueryList<ElementRef>
  visible = signal(new Set<number>())
  observer?: IntersectionObserver

  features: FeatureItem[] = [
    { icon: '🖥️', title: 'Multi-Device Streaming', description: 'Watch on any device - TV, laptop, tablet, or smartphone with seamless synchronization.' },
    { icon: '⚡', title: 'Ultra-Low Latency', description: 'Experience real-time action with our advanced streaming technology and minimal delay.' },
    { icon: '🛡️', title: 'Premium Quality', description: '4K Ultra HD streaming with HDR support for the most immersive viewing experience.' },
    { icon: '👥', title: 'Social Features', description: 'Connect with fellow fans, share moments, and engage in live discussions during matches.' },
    { icon: '📺', title: 'Smart TV Apps', description: 'Native apps for all major smart TV platforms with intuitive remote control navigation.' },
    { icon: '📱', title: 'Mobile Optimized', description: 'Optimized mobile experience with offline downloads and data-saving streaming options.' },
  ]

  ngOnInit(): void {
    this.observer = new IntersectionObserver((entries) => {
      const next = new Set(this.visible())
      entries.forEach(entry => {
        const indexAttr = (entry.target as HTMLElement).getAttribute('data-index')
        const idx = indexAttr ? parseInt(indexAttr, 10) : -1
        if (idx >= 0 && entry.isIntersecting) {
          next.add(idx)
        }
      })
      this.visible.set(next)
    }, { threshold: 0.1 })
  }

  ngOnDestroy(): void {
    this.observer?.disconnect()
  }

  isVisible(i: number) {
    return this.visible().has(i)
  }
}
