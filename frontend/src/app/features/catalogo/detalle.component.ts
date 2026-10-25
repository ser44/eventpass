import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { interval, switchMap } from 'rxjs';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatChipsModule } from '@angular/material/chips';
import { EventoService } from '../../core/eventos/evento.service';
import { Evento } from '../../core/models/evento.model';
import { AuthService } from '../../core/auth/auth.service';
import { ReservaService } from '../../core/reservas/reserva.service';
import { LoadingComponent } from '../../shared/loading.component';
import { EmptyStateComponent } from '../../shared/empty-state.component';

@Component({
  selector: 'app-detalle',
  standalone: true,
  imports: [
    DatePipe, RouterLink, MatCardModule, MatIconModule, MatButtonModule, MatButtonToggleModule, MatChipsModule,
    LoadingComponent, EmptyStateComponent,
  ],
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

  readonly opciones = [1, 2, 3, 4];

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
        this.cargar(); // refresca el cupo (pudo haberse agotado)
      },
    });
  }

  private cargar(): void {
    this.cargando.set(true);
    this.error.set(false);
    this.service.detalle(this.id).subscribe({
      next: (e) => {
        this.evento.set(e);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set(true);
        this.cargando.set(false);
      },
    });
  }
}
