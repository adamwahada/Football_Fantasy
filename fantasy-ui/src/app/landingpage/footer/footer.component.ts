import { Component } from '@angular/core'
import { CommonModule } from '@angular/common'

@Component({
  selector: 'sportstream-footer',
  standalone: true,
  imports: [CommonModule],
  template: `
    <footer class="bg-background border-t border-\[\#0dfd86\]\/20">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div class="grid grid-cols-1 md:grid-cols-4 gap-8">
          <div class="col-span-1 md:col-span-2">
            <div class="flex items-center space-x-2 mb-4">
              <div class="w-8 h-8 bg-\[\#0dfd86\] rounded-lg flex items-center justify-center">
                <span class="w-4 h-4 text-\[\#0a0f0a\] inline-block">▶</span>
              </div>
              <span class="text-xl font-bold text-foreground">SPORTSTREAM</span>
            </div>
            <p class="text-foreground/80 mb-6 max-w-md">
              The ultimate destination for live sports streaming. Experience every game, every moment, in stunning quality.
            </p>
            <div class="flex space-x-4">
              <a href="#" class="text-foreground/60 hover:text-\[\#0dfd86\] transition-colors">f</a>
              <a href="#" class="text-foreground/60 hover:text-\[\#0dfd86\] transition-colors">t</a>
              <a href="#" class="text-foreground/60 hover:text-\[\#0dfd86\] transition-colors">ig</a>
              <a href="#" class="text-foreground/60 hover:text-\[\#0dfd86\] transition-colors">yt</a>
            </div>
          </div>

          <div>
            <h3 class="text-lg font-semibold text-foreground mb-4">Quick Links</h3>
            <ul class="space-y-2">
              <li><a href="#" class="text-foreground/80 hover:text-\[\#0dfd86\] transition-colors">Live Sports</a></li>
              <li><a href="#" class="text-foreground/80 hover:text-\[\#0dfd86\] transition-colors">Schedule</a></li>
              <li><a href="#" class="text-foreground/80 hover:text-\[\#0dfd86\] transition-colors">Highlights</a></li>
              <li><a href="#" class="text-foreground/80 hover:text-\[\#0dfd86\] transition-colors">News</a></li>
            </ul>
          </div>

          <div>
            <h3 class="text-lg font-semibold text-foreground mb-4">Support</h3>
            <ul class="space-y-2">
              <li><a href="#" class="text-foreground/80 hover:text-\[\#0dfd86\] transition-colors">Help Center</a></li>
              <li><a href="#" class="text-foreground/80 hover:text-\[\#0dfd86\] transition-colors">Contact Us</a></li>
              <li><a href="#" class="text-foreground/80 hover:text-\[\#0dfd86\] transition-colors">Privacy Policy</a></li>
              <li><a href="#" class="text-foreground/80 hover:text-\[\#0dfd86\] transition-colors">Terms of Service</a></li>
            </ul>
          </div>
        </div>

        <div class="border-t border-\[\#0dfd86\]\/20 mt-8 pt-8 text-center">
        <p class="text-foreground/60"> 2024 SportStream. All rights reserved. | Contact: supportsportstream.com | Phone: +1 (555) 123-4567</p>
        </div>
      </div>
    </footer>
  `,
})
export class FooterComponent {}
