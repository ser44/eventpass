import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { EventoService } from '../../core/eventos/evento.service';
import { EventoResumen } from '../../core/models/evento.model';
import { CheckinService } from '../../core/checkin/checkin.service';
import { Stats } from '../../core/models/checkin.model';
import { LoadingComponent } from '../../shared/loading.component';
import { EmptyStateComponent } from '../../shared/empty-state.component';
import { ConfirmDialogComponent } from '../../shared/confirm-dialog.component';

@Component({
  selector: 'app-panel',
  standalone: true,
  imports: [
    DatePipe, RouterLink, MatCardModule, MatButtonModule, MatIconModule, MatChipsModule,
    LoadingComponent, EmptyStateComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './panel.component.html',
  styleUrl: './panel.component.scss',
})
export class PanelComponent implements OnInit {
  private readonly service = inject(EventoService);
  private readonly checkin = inject(CheckinService);
  private readonly snack = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);

  readonly eventos = signal<EventoResumen[]>([]);
  readonly cargando = signal(true);
  readonly error = signal(false);
  readonly stats = signal<Record<string, Stats>>({});

  verStats(e: EventoResumen): void {
    if (this.stats()[e.id]) {
      const copia = { ...this.stats() };
      delete copia[e.id];
      this.stats.set(copia);
      return;
    }
    this.checkin.stats(e.id).subscribe({
      next: (s) => this.stats.set({ ...this.stats(), [e.id]: s }),
    });
  }

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set(false);
    this.service.misEventos().subscribe({
      next: (e) => {
        this.eventos.set(e);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set(true);
        this.cargando.set(false);
      },
    });
  }

  publicar(e: EventoResumen): void {
    this.service.publicar(e.id).subscribe({
      next: () => {
        this.snack.open('Evento publicado', 'Cerrar', { duration: 3000 });
        this.cargar();
      },
    });
  }

  cancelar(e: EventoResumen): void {
    this.dialog
      .open(ConfirmDialogComponent, {
        data: { titulo: 'Cancelar evento', mensaje: `¿Cancelar "${e.titulo}"?`, confirmar: 'Cancelar evento' },
      })
      .afterClosed()
      .subscribe((ok) => {
        if (ok) {
          this.service.cancelar(e.id).subscribe({
            next: () => {
              this.snack.open('Evento cancelado', 'Cerrar', { duration: 3000 });
              this.cargar();
            },
          });
        }
      });
  }
}
