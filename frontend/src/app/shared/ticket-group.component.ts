import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Entrada } from '../core/models/reserva.model';
import { QrViewComponent } from './qr-view.component';
import { bannerPara, diaDe, horaDe, mesCorto } from './banner.util';

export interface GrupoReserva {
  reservaId: string;
  eventoId: string;
  eventoTitulo: string;
  eventoFecha: string;
  eventoLugar: string;
  entradas: Entrada[];
  cancelable: boolean;
}

/** Tarjeta de una reserva: banner del evento + QR de sus entradas en carrusel. */
@Component({
  selector: 'app-ticket-group',
  standalone: true,
  imports: [MatButtonModule, MatIconModule, QrViewComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="ticket">
      <div class="banner" [style.background]="banner().bg" [style.color]="banner().fg">
        <span class="fecha">{{ dia() }} {{ mes() }} · {{ hora() }} h</span>
        <h3>{{ grupo().eventoTitulo }}</h3>
        <span class="lugar" [style.color]="banner().sub">{{ grupo().eventoLugar }}</span>
      </div>

      <div class="cuerpo">
        @if (actual().estado === 'VALIDA') {
          <app-qr-view [codigo]="actual().codigo" [ancho]="220"></app-qr-view>
        } @else {
          <div class="inactivo">
            <mat-icon>block</mat-icon>
            <code>{{ actual().codigo }}</code>
          </div>
        }
        <span class="chip" [class]="'estado-' + actual().estado">{{ actual().estado }}</span>

        @if (grupo().entradas.length > 1) {
          <div class="nav">
            <button mat-stroked-button (click)="prev()" aria-label="Anterior"><mat-icon>chevron_left</mat-icon></button>
            <span>Entrada {{ indice() + 1 }} de {{ grupo().entradas.length }}</span>
            <button mat-stroked-button (click)="next()" aria-label="Siguiente"><mat-icon>chevron_right</mat-icon></button>
          </div>
        }
      </div>

      <p class="ayuda">Mostrá este código en la puerta. Cada entrada se usa una sola vez.</p>

      @if (grupo().cancelable) {
        <button mat-button class="cancelar" (click)="cancelar.emit()">Cancelar reserva</button>
      }
    </div>
  `,
  styles: [`
    .ticket { background: var(--ep-surface); border: 1px solid var(--ep-border); border-radius: 18px;
              overflow: hidden; margin-bottom: 16px; }
    .banner { padding: 16px 18px; display: flex; flex-direction: column; }
    .fecha { font: 600 12px/1 'Space Grotesk', sans-serif; letter-spacing: 1px; text-transform: uppercase; }
    .banner h3 { margin: 4px 0 2px; font-size: 1.3rem; }
    .lugar { font-size: 14px; }
    .cuerpo { display: flex; flex-direction: column; align-items: center; gap: 12px; padding: 20px; }
    .inactivo { display: flex; flex-direction: column; align-items: center; gap: 8px; padding: 40px;
                background: var(--ep-bg); border-radius: 12px; opacity: .7; }
    .inactivo mat-icon { font-size: 44px; width: 44px; height: 44px; }
    .chip { font-size: 12px; font-weight: 700; padding: 4px 12px; border-radius: 999px; color: #fff; }
    .estado-VALIDA { background: #0f6b37; }
    .estado-USADA { background: #5a5768; }
    .estado-ANULADA { background: #a3201a; }
    .nav { display: flex; align-items: center; gap: 16px; font-weight: 500; }
    .ayuda { text-align: center; color: var(--texto-tenue); font-size: 13px; margin: 0 16px 8px;
             padding-top: 12px; border-top: 1px dashed var(--ep-border); }
    .cancelar { display: block; margin: 0 auto 12px; color: var(--ep-error); font-weight: 600; }
  `],
})
export class TicketGroupComponent {
  readonly grupo = input.required<GrupoReserva>();
  readonly cancelar = output<void>();

  readonly indice = signal(0);
  readonly actual = computed(() => this.grupo().entradas[Math.min(this.indice(), this.grupo().entradas.length - 1)]);
  readonly banner = computed(() => bannerPara(this.grupo().eventoId));
  readonly mes = computed(() => mesCorto(this.grupo().eventoFecha));
  readonly dia = computed(() => diaDe(this.grupo().eventoFecha));
  readonly hora = computed(() => horaDe(this.grupo().eventoFecha));

  prev(): void {
    this.indice.update((i) => (i - 1 + this.grupo().entradas.length) % this.grupo().entradas.length);
  }

  next(): void {
    this.indice.update((i) => (i + 1) % this.grupo().entradas.length);
  }
}
