import { Component, computed, inject } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatMenuModule } from '@angular/material/menu';
import { AuthService } from './core/auth/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    RouterOutlet, RouterLink, RouterLinkActive,
    MatToolbarModule, MatSidenavModule, MatButtonModule, MatIconModule, MatListModule, MatMenuModule,
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent {
  private readonly breakpoints = inject(BreakpointObserver);
  readonly auth = inject(AuthService);

  // true en celular/tablet chico: el sidenav pasa a modo "over" con hamburguesa.
  readonly esCompacto = toSignal(
    this.breakpoints
      .observe([Breakpoints.XSmall, Breakpoints.Small])
      .pipe(map((r) => r.matches)),
    { initialValue: false },
  );

  // El menú cambia según el rol del usuario.
  readonly links = computed(() => {
    const u = this.auth.usuario();
    const items = [{ ruta: '/eventos', texto: 'Eventos', icono: 'event' }];
    if (u) {
      items.push({ ruta: '/mis-entradas', texto: 'Mis entradas', icono: 'confirmation_number' });
    }
    if (u?.rol === 'ORGANIZADOR') {
      items.push({ ruta: '/organizador', texto: 'Mis eventos', icono: 'dashboard' });
    }
    return items;
  });

  logout(): void {
    this.auth.logout();
  }
}
