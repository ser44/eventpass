import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  imports: [MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="vacio">
      <mat-icon>{{ icono() }}</mat-icon>
      <p>{{ mensaje() }}</p>
    </div>
  `,
  styles: [`
    .vacio { display: flex; flex-direction: column; align-items: center; gap: 8px;
             padding: 48px 16px; color: var(--texto-tenue); text-align: center; }
    mat-icon { font-size: 48px; width: 48px; height: 48px; }
  `],
})
export class EmptyStateComponent {
  readonly icono = input('inbox');
  readonly mensaje = input('No hay nada para mostrar');
}
