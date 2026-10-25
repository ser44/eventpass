import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ReservaService } from '../../core/reservas/reserva.service';
import { Reserva } from '../../core/models/reserva.model';
import { LoadingComponent } from '../../shared/loading.component';
import { EmptyStateComponent } from '../../shared/empty-state.component';
import { CountdownComponent } from '../../shared/countdown.component';

@Component({
  selector: 'app-confirmacion',
  standalone: true,
  imports: [
    MatCardModule, MatButtonModule, MatIconModule, LoadingComponent, EmptyStateComponent, CountdownComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './confirmacion.component.html',
  styleUrl: './confirmacion.component.scss',
})
export class ConfirmacionComponent implements OnInit {
  private readonly service = inject(ReservaService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly snack = inject(MatSnackBar);

  readonly reserva = signal<Reserva | null>(null);
  readonly cargando = signal(true);
  readonly error = signal(false);
  readonly confirmando = signal(false);
  readonly vencida = signal(false);

  private id = '';

  ngOnInit(): void {
    this.id = this.route.snapshot.paramMap.get('id') ?? '';
    this.service.detalle(this.id).subscribe({
      next: (r) => {
        this.reserva.set(r);
        this.cargando.set(false);
        if (r.estado === 'CONFIRMADA') {
          this.router.navigate(['/mis-entradas']);
        } else if (r.estado !== 'PENDIENTE') {
          this.vencida.set(true);
        }
      },
      error: () => {
        this.error.set(true);
        this.cargando.set(false);
      },
    });
  }

  confirmar(): void {
    if (this.confirmando() || this.vencida()) {
      return;
    }
    this.confirmando.set(true);
    this.service.confirmar(this.id).subscribe({
      next: () => {
        this.snack.open('¡Reserva confirmada! Ya tenés tus entradas.', 'Cerrar', { duration: 4000 });
        this.router.navigate(['/mis-entradas']);
      },
      error: () => {
        this.confirmando.set(false);
        const r = this.reserva();
        if (r) {
          this.router.navigate(['/eventos', r.eventoId]);
        }
      },
    });
  }

  alVencer(): void {
    this.vencida.set(true);
  }
}
