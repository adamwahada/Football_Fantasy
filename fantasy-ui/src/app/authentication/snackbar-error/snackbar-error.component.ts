import { Component, Inject } from '@angular/core';
import { MAT_SNACK_BAR_DATA } from '@angular/material/snack-bar';

@Component({
  selector: 'app-snackbar-error',
  template: `
    <div class="snackbar-error-content">
      <span class="snackbar-icon">
        <svg width="24" height="24" fill="#d32f2f" viewBox="0 0 24 24">
          <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10
                    10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/>
        </svg>
      </span>
      <span class="snackbar-text">{{ data }}</span>
    </div>
  `,
  styles: [`


    .snackbar-icon {
      margin-right: 12px;
      display: flex;
      align-items: center;
      flex-shrink: 0;
    }

    .snackbar-text {
      flex: 1;
      line-height: 1.4;
      word-wrap: break-word;
    }

    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(10px); }
      to   { opacity: 1; transform: translateY(0); }
    }
  `]
})
export class SnackbarErrorComponent {
  constructor(@Inject(MAT_SNACK_BAR_DATA) public data: string) {}
}