import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatRippleModule } from '@angular/material/core';
import { EventoResumen } from '../core/models/evento.model';
import { bannerPara, diaDe, horaDe, mesCorto } from './banner.util';

@Component({
  selector: 'app-event-card',
  standalone: true,
  imports: [RouterLink, MatRippleModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <a class="card" matRipple [routerLink]="['/eventos', evento().id]">
      <div class="banner" [style.background]="banner().bg" [style.color]="banner().fg">
        <span class="mes">{{ mes() }}</span>
        <span class="dia">{{ dia() }}</span>
        @if (ultimos()) {
          <span class="pill">Últimos lugares</span>
        }
      </div>
      <div class="cuerpo">
        <h3 class="titulo">{{ evento().titulo }}</h3>
        <p class="meta">{{ evento().lugar }} · {{ hora() }} h</p>
        <div class="barra"><span [style.width.%]="ocupadoPct()" [class.urgente]="ultimos()"></span></div>
        <p class="restante" [class.agotado]="evento().cupoDisponible === 0">
          @if (evento().cupoDisponible === 0) { Agotado }
          @else { Quedan {{ evento().cupoDisponible }} de {{ evento().cupoTotal }} }
        </p>
      </div>
    </a>
  `,
  styles: [`
    .card { display: flex; flex-direction: column; height: 100%; text-decoration: none; color: inherit;
            background: var(--ep-surface); border: 1px solid var(--ep-border); border-radius: 18px;
            overflow: hidden; transition: box-shadow .15s, transform .15s; }
    .card:hover { box-shadow: 0 6px 20px rgba(23,21,31,.12); transform: translateY(-2px); }
    .banner { position: relative; padding: 18px 18px 22px; display: flex; flex-direction: column; min-height: 110px; }
    .mes { font: 600 12px/1 'Space Grotesk', sans-serif; letter-spacing: 1.5px; }
    .dia { font: 700 44px/1 'Space Grotesk', sans-serif; margin-top: 2px; }
    .pill { position: absolute; top: 14px; right: 14px; background: #fbe7c6; color: #5b3200;
            font-size: 11px; font-weight: 600; padding: 4px 10px; border-radius: 999px; }
    .cuerpo { flex: 1; padding: 16px 18px 18px; display: flex; flex-direction: column; gap: 6px; }
    .titulo { margin: 0; font-size: 1.1rem; }
    .meta { margin: 0; color: var(--texto-tenue); font-size: 14px; }
    .barra { height: 6px; border-radius: 999px; background: var(--ep-border); overflow: hidden; margin-top: 6px; }
    .barra span { display: block; height: 100%; background: var(--ep-primary); border-radius: 999px; }
    .barra span.urgente { background: #b45309; }
    .restante { margin: 2px 0 0; font-size: 13px; color: var(--texto-tenue); font-weight: 500; }
    .restante.agotado { color: var(--ep-error); }
  `],
})
export class EventCardComponent {
  readonly evento = input.required<EventoResumen>();

  readonly mes = computed(() => mesCorto(this.evento().fechaInicio));
  readonly dia = computed(() => diaDe(this.evento().fechaInicio));
  readonly hora = computed(() => horaDe(this.evento().fechaInicio));
  readonly banner = computed(() => bannerPara(this.evento().id));
  readonly ultimos = computed(() => {
    const e = this.evento();
    return e.cupoDisponible > 0 && e.cupoDisponible <= Math.max(5, e.cupoTotal * 0.1);
  });
  readonly ocupadoPct = computed(() => {
    const e = this.evento();
    return e.cupoTotal ? Math.round(((e.cupoTotal - e.cupoDisponible) / e.cupoTotal) * 100) : 0;
  });
}
