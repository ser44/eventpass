import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar } from '@angular/material/snack-bar';
import { EventoService } from '../../core/eventos/evento.service';
import { LoadingComponent } from '../../shared/loading.component';

@Component({
  selector: 'app-evento-form',
  standalone: true,
  imports: [
    ReactiveFormsModule, MatCardModule, MatFormFieldModule, MatInputModule, MatButtonModule, LoadingComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './evento-form.component.html',
  styleUrl: './evento-form.component.scss',
})
export class EventoFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(EventoService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly snack = inject(MatSnackBar);

  readonly modoEditar = signal(false);
  readonly cargando = signal(false);
  readonly enviando = signal(false);

  private id: string | null = null;
  private version = 0;

  readonly form = this.fb.nonNullable.group({
    titulo: ['', [Validators.required, Validators.maxLength(160)]],
    lugar: ['', [Validators.required, Validators.maxLength(200)]],
    fechaInicio: ['', [Validators.required]],
    cupoTotal: [50, [Validators.required, Validators.min(1)]],
    descripcion: [''],
    imagenUrl: [''],
  });

  ngOnInit(): void {
    this.id = this.route.snapshot.paramMap.get('id');
    if (this.id) {
      this.modoEditar.set(true);
      this.cargando.set(true);
      this.service.detalle(this.id).subscribe({
        next: (e) => {
          this.version = e.version;
          this.form.patchValue({
            titulo: e.titulo,
            lugar: e.lugar,
            fechaInicio: this.aInputDateTime(e.fechaInicio),
            cupoTotal: e.cupoTotal,
            descripcion: e.descripcion ?? '',
            imagenUrl: e.imagenUrl ?? '',
          });
          this.cargando.set(false);
        },
        error: () => this.cargando.set(false),
      });
    }
  }

  enviar(): void {
    if (this.form.invalid || this.enviando()) {
      return;
    }
    this.enviando.set(true);
    const v = this.form.getRawValue();
    const base = {
      titulo: v.titulo,
      lugar: v.lugar,
      fechaInicio: new Date(v.fechaInicio).toISOString(),
      cupoTotal: v.cupoTotal,
      descripcion: v.descripcion || undefined,
      imagenUrl: v.imagenUrl || undefined,
    };
    const peticion = this.id
      ? this.service.actualizar(this.id, { ...base, version: this.version })
      : this.service.crear(base);

    peticion.subscribe({
      next: () => {
        this.snack.open(this.id ? 'Evento actualizado' : 'Evento creado', 'Cerrar', { duration: 3000 });
        this.router.navigate(['/organizador']);
      },
      error: () => this.enviando.set(false),
    });
  }

  private aInputDateTime(iso: string): string {
    const d = new Date(iso);
    const p = (n: number) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}T${p(d.getHours())}:${p(d.getMinutes())}`;
  }
}
