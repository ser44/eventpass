import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AdminService } from '../../core/admin/admin.service';
import { Rol, Usuario } from '../../core/models/usuario.model';
import { LoadingComponent } from '../../shared/loading.component';
import { EmptyStateComponent } from '../../shared/empty-state.component';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [
    DatePipe, FormsModule, MatCardModule, MatFormFieldModule, MatSelectModule,
    MatSlideToggleModule, MatIconModule, LoadingComponent, EmptyStateComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.scss',
})
export class AdminComponent implements OnInit {
  private readonly service = inject(AdminService);
  private readonly snack = inject(MatSnackBar);

  readonly usuarios = signal<Usuario[]>([]);
  readonly cargando = signal(true);
  readonly error = signal(false);
  readonly roles: Rol[] = ['ASISTENTE', 'ORGANIZADOR', 'ADMIN'];

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando.set(true);
    this.error.set(false);
    this.service.listar().subscribe({
      next: (u) => {
        this.usuarios.set(u);
        this.cargando.set(false);
      },
      error: () => {
        this.error.set(true);
        this.cargando.set(false);
      },
    });
  }

  cambiarRol(u: Usuario, rol: Rol): void {
    this.service.actualizar(u.id, { rol }).subscribe({
      next: (actualizado) => {
        this.reemplazar(actualizado);
        this.snack.open('Rol actualizado', 'Cerrar', { duration: 2500 });
      },
    });
  }

  cambiarActivo(u: Usuario, activo: boolean): void {
    this.service.actualizar(u.id, { activo }).subscribe({
      next: (actualizado) => {
        this.reemplazar(actualizado);
        this.snack.open(activo ? 'Usuario activado' : 'Usuario desactivado', 'Cerrar', { duration: 2500 });
      },
    });
  }

  private reemplazar(u: Usuario): void {
    this.usuarios.update((lista) => lista.map((x) => (x.id === u.id ? u : x)));
  }
}
