import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-loading',
  standalone: true,
  imports: [MatProgressSpinnerModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="loading">
      <mat-spinner diameter="40"></mat-spinner>
    </div>
  `,
  styles: [`.loading { display: flex; justify-content: center; padding: 48px; }`],
})
export class LoadingComponent {}
