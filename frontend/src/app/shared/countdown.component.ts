import {
  ChangeDetectionStrategy, Component, DestroyRef, OnInit, computed, inject, input, output, signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { interval } from 'rxjs';

/** Cuenta regresiva hasta `expiraEn` (ISO). Emite `expirado` una sola vez al llegar a 0. */
@Component({
  selector: 'app-countdown',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span class="cd" [class.urgente]="restante() <= 60 && restante() > 0">{{ etiqueta() }}</span>`,
  styles: [`
    .cd { font-variant-numeric: tabular-nums; font-weight: 600; font-size: 1.3rem; }
    .urgente { color: #c62828; }
  `],
})
export class CountdownComponent implements OnInit {
  readonly expiraEn = input.required<string>();
  readonly expirado = output<void>();

  readonly restante = signal(0);
  readonly etiqueta = computed(() => {
    const s = this.restante();
    if (s <= 0) {
      return 'Vencida';
    }
    const m = Math.floor(s / 60);
    const seg = s % 60;
    return `${String(m).padStart(2, '0')}:${String(seg).padStart(2, '0')}`;
  });

  private readonly destroyRef = inject(DestroyRef);
  private yaEmitido = false;

  ngOnInit(): void {
    this.calcular();
    interval(1000).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.calcular());
  }

  private calcular(): void {
    const ms = new Date(this.expiraEn()).getTime() - Date.now();
    const s = Math.max(0, Math.floor(ms / 1000));
    this.restante.set(s);
    if (s === 0 && !this.yaEmitido) {
      this.yaEmitido = true;
      this.expirado.emit();
    }
  }
}
