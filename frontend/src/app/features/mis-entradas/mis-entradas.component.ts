import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { ReservaService } from '../../core/reservas/reserva.service';
import { Entrada } from '../../core/models/reserva.model';
import { LoadingComponent } from '../../shared/loading.component';
import { EmptyStateComponent } from '../../shared/empty-state.component';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';
import { GrupoReserva, TicketGroupComponent } from '../../shared/ticket-group.component';

@Component({
  selector: 'app-mis-entradas',
  standalone: true,
  imports: [LoadingComponent, EmptyStateComponent, TicketGroupComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './mis-entradas.component.html',
  styleUrl: './mis-entradas.component.scss',
})
export class MisEntradasComponent implements OnInit {
  private readonly service = inject(ReservaService);
  private readonly snack = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  readonly entradas = signal<Entrada[]>([]);
  readonly cargando = signal(true);
  readonly error = signal(false);

  readonly grupos = computed<GrupoReserva[]>(() => {
    const porReserva = new Map<string, Entrada[]>();
    for (const e of this.entradas()) {
      const lista = porReserva.get(e.reservaId) ?? [];
      lista.push(e);
      porReserva.set(e.reservaId, lista);
    }
    return Array.from(porReserva.entries()).map(([reservaId, entradas]) => ({
      reservaId,
      eventoId: entradas[0].eventoId,
      eventoTitulo: entradas[0].eventoTitulo,
      eventoFecha: entradas[0].eventoFecha,
      eventoLugar: entradas[0].eventoLugar,
      entradas,
      cancelable: entradas.every((e) => e.estado === 'VALIDA'),
    }));
  });

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set(false);
    this.service.misEntradas().subscribe({
      next: (e) => {
        this.entradas.set(e);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set(true);
        this.cargando.set(false);
      },
    });
  }

  cancelar(g: GrupoReserva): void {
    this.dialog
      .open(ConfirmDialogComponent, {
        data: {
          titulo: 'Cancelar reserva',
          mensaje: `¿Cancelar tus ${g.entradas.length} entrada(s) para "${g.eventoTitulo}"?`,
          confirmar: 'Cancelar reserva',
        },
      })
      .afterClosed()
      .subscribe((ok) => {
        if (ok) {
          this.service.cancelar(g.reservaId).subscribe({
            next: () => {
              this.snack.open('Reserva cancelada', 'Cerrar', { duration: 3000 });
              this.cargar();
            },
          });
        }
      });
  }
}
