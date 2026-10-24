import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { EventoService } from '../../core/eventos/evento.service';
import { EventoResumen } from '../../core/models/evento.model';
import { EventCardComponent } from '../../shared/event-card.component';
import { LoadingComponent } from '../../shared/loading.component';
import { EmptyStateComponent } from '../../shared/empty-state.component';

@Component({
  selector: 'app-catalogo',
  standalone: true,
  imports: [
    ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatIconModule, MatButtonModule,
    EventCardComponent, LoadingComponent, EmptyStateComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './catalogo.component.html',
  styleUrl: './catalogo.component.scss',
})
export class CatalogoComponent implements OnInit {
  private readonly service = inject(EventoService);
  private readonly destroyRef = inject(DestroyRef);

  readonly eventos = signal<EventoResumen[]>([]);
  readonly cargando = signal(false);
  readonly error = signal(false);
  readonly pagina = signal(0);
  readonly totalPaginas = signal(0);

  readonly busqueda = new FormControl('', { nonNullable: true });

  ngOnInit(): void {
    this.cargar();
    this.busqueda.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.pagina.set(0);
        this.cargar();
      });
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set(false);
    this.service.catalogo({ q: this.busqueda.value, page: this.pagina() }).subscribe({
      next: (p) => {
        this.eventos.set(p.contenido);
        this.totalPaginas.set(p.totalPaginas);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set(true);
        this.cargando.set(false);
      },
    });
  }

  irA(pagina: number): void {
    this.pagina.set(pagina);
    this.cargar();
  }
}
