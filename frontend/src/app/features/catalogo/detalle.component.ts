import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { interval, switchMap } from 'rxjs';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { EventoService } from '../../core/eventos/evento.service';
import { Evento } from '../../core/models/evento.model';
import { LoadingComponent } from '../../shared/loading.component';
import { EmptyStateComponent } from '../../shared/empty-state.component';

@Component({
  selector: 'app-detalle',
  standalone: true,
  imports: [
    DatePipe, RouterLink, MatCardModule, MatIconModule, MatButtonModule, MatChipsModule,
    LoadingComponent, EmptyStateComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './detalle.component.html',
  styleUrl: './detalle.component.scss',
})
export class DetalleComponent implements OnInit {
  private readonly service = inject(EventoService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  readonly evento = signal<Evento | null>(null);
  readonly cargando = signal(true);
  readonly error = signal(false);

  private id = '';

  ngOnInit(): void {
    this.id = this.route.snapshot.paramMap.get('id') ?? '';
    this.cargar();
    // Refresco del cupo cada 15 s (la fuente de verdad es el backend).
    interval(15000)
      .pipe(switchMap(() => this.service.detalle(this.id)), takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: (e) => this.evento.set(e), error: () => {} });
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
