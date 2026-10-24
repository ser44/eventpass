import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { EventoResumen } from '../core/models/evento.model';

/** Card presentacional para el catálogo. Solo recibe datos por input. */
@Component({
  selector: 'app-event-card',
  standalone: true,
  imports: [DatePipe, RouterLink, MatCardModule, MatIconModule, MatButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <mat-card class="card">
      @if (evento().imagenUrl) {
        <img [src]="evento().imagenUrl" [alt]="evento().titulo" class="imagen" />
      }
      <mat-card-content class="cuerpo">
        <h3 class="titulo">{{ evento().titulo }}</h3>
        <p class="meta"><mat-icon>place</mat-icon> {{ evento().lugar }}</p>
        <p class="meta"><mat-icon>event</mat-icon> {{ evento().fechaInicio | date: 'medium' }}</p>
        <p class="cupo" [class.agotado]="evento().cupoDisponible === 0">
          {{ evento().cupoDisponible }} / {{ evento().cupoTotal }} lugares
        </p>
      </mat-card-content>
      <mat-card-actions align="end">
        <a mat-button color="primary" [routerLink]="['/eventos', evento().id]">Ver detalle</a>
      </mat-card-actions>
    </mat-card>
  `,
  styles: [`
    .card { display: flex; flex-direction: column; height: 100%; }
    .imagen { width: 100%; height: 150px; object-fit: cover; }
    .cuerpo { flex: 1; }
    .titulo { margin: 0 0 8px; overflow-wrap: anywhere; }
    .meta { display: flex; align-items: center; gap: 6px; margin: 4px 0; color: rgba(0,0,0,.7); font-size: 14px; }
    .meta mat-icon { font-size: 18px; width: 18px; height: 18px; }
    .cupo { font-weight: 500; margin-top: 8px; }
    .cupo.agotado { color: #c62828; }
  `],
})
export class EventCardComponent {
  readonly evento = input.required<EventoResumen>();
}
