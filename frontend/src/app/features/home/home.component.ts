import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { environment } from '../../../environments/environment';

interface HolaApi {
  servicio: string;
  mensaje: string;
  hora: string;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [MatCardModule, MatButtonModule, MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
})
export class HomeComponent {
  private readonly http = inject(HttpClient);

  readonly estado = signal<'inicial' | 'cargando' | 'ok' | 'error'>('inicial');
  readonly respuesta = signal<HolaApi | null>(null);

  probarBackend(): void {
    this.estado.set('cargando');
    this.http.get<HolaApi>(`${environment.apiUrl}/hola`).subscribe({
      next: (r) => {
        this.respuesta.set(r);
        this.estado.set('ok');
      },
      error: () => this.estado.set('error'),
    });
  }
}
