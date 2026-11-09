import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { ActivatedRoute } from '@angular/router';
import { interval, switchMap } from 'rxjs';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { EventoService } from '../../core/eventos/evento.service';
import { Evento } from '../../core/models/evento.model';
import { AuthService } from '../../core/auth/auth.service';
import { ReservaService } from '../../core/reservas/reserva.service';
import { LoadingComponent } from '../../shared/loading.component';
import { EmptyStateComponent } from '../../shared/empty-state.component';
import { bannerPara, horaDe } from '../../shared/banner.util';

@Component({
  selector: 'app-detalle',
  standalone: true,
  imports: [MatIconModule, MatButtonModule, LoadingComponent, EmptyStateComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './detalle.component.html',
  styleUrl: './detalle.component.scss',
})
export class DetalleComponent implements OnInit {
  private readonly service = inject(EventoService);
  private readonly reservas = inject(ReservaService);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly evento = signal<Evento | null>(null);
  readonly cargando = signal(true);
  readonly error = signal(false);
  readonly cantidad = signal(1);
  readonly reservando = signal(false);

  readonly banner = computed(() => {
    const e = this.evento();
    return e ? bannerPara(e.id) : null;
  });
  readonly fechaLarga = computed(() => {
    const e = this.evento();
    if (!e) return '';
    const d = new Date(e.fechaInicio);
    const s = d.toLocaleDateString('es-AR', { weekday: 'long', day: 'numeric', month: 'long' });
    return s.charAt(0).toUpperCase() + s.slice(1);
  });
  readonly hora = computed(() => (this.evento() ? horaDe(this.evento()!.fechaInicio) : ''));
  readonly ocupadoPct = computed(() => {
    const e = this.evento();
    return e && e.cupoTotal ? Math.round(((e.cupoTotal - e.cupoDisponible) / e.cupoTotal) * 100) : 0;
  });
  readonly ultimos = computed(() => {
    const e = this.evento();
    return !!e && e.cupoDisponible > 0 && e.cupoDisponible <= Math.max(5, e.cupoTotal * 0.1);
  });
  readonly maximo = computed(() => Math.min(4, this.evento()?.cupoDisponible ?? 1));
  readonly puedeReservar = computed(() => {
    const e = this.evento();
    return !!e && e.estado === 'PUBLICADO' && e.cupoDisponible > 0 && new Date(e.fechaInicio) > new Date();
  });

  private id = '';

  ngOnInit(): void {
    this.id = this.route.snapshot.paramMap.get('id') ?? '';
    this.cargar();
    interval(15000)
      .pipe(switchMap(() => this.service.detalle(this.id)), takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: (e) => this.evento.set(e), error: () => {} });
  }

  menos(): void {
    this.cantidad.update((n) => Math.max(1, n - 1));
  }

  mas(): void {
    this.cantidad.update((n) => Math.min(this.maximo(), n + 1));
  }

  volver(): void {
    this.router.navigate(['/eventos']);
  }

  reservar(): void {
    if (!this.auth.estaAutenticado()) {
      this.router.navigate(['/login'], { queryParams: { redirect: `/eventos/${this.id}` } });
      return;
    }
    if (this.reservando()) {
      return;
    }
    this.reservando.set(true);
    const idempotencyKey = crypto.randomUUID();
    this.reservas.reservar(this.id, this.cantidad(), idempotencyKey).subscribe({
      next: (r) => this.router.navigate(['/reservas', r.id]),
      error: () => {
        this.reservando.set(false);
        this.cargar();
      },
    });
  }

  private cargar(): void {
    this.cargando.set(true);
    this.error.set(false);
    this.service.detalle(this.id).subscribe({
      next: (e) => {
        this.evento.set(e);
        this.cantidad.update((n) => Math.min(n, this.maximo()));
        this.cargando.set(false);
      },
      error: () => {
        this.error.set(true);
        this.cargando.set(false);
      },
    });
  }
}
