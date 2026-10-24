import { Routes } from '@angular/router';
import { roleGuard } from './core/auth/role.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'eventos', pathMatch: 'full' },
  {
    path: 'eventos',
    loadComponent: () =>
      import('./features/catalogo/catalogo.component').then((m) => m.CatalogoComponent),
  },
  {
    path: 'eventos/:id',
    loadComponent: () =>
      import('./features/catalogo/detalle.component').then((m) => m.DetalleComponent),
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'registro',
    loadComponent: () => import('./features/auth/registro.component').then((m) => m.RegistroComponent),
  },
  {
    path: 'organizador',
    canActivate: [roleGuard],
    data: { roles: ['ORGANIZADOR'] },
    loadComponent: () =>
      import('./features/organizador/panel.component').then((m) => m.PanelComponent),
  },
  {
    path: 'organizador/eventos/nuevo',
    canActivate: [roleGuard],
    data: { roles: ['ORGANIZADOR'] },
    loadComponent: () =>
      import('./features/organizador/evento-form.component').then((m) => m.EventoFormComponent),
  },
  {
    path: 'organizador/eventos/:id/editar',
    canActivate: [roleGuard],
    data: { roles: ['ORGANIZADOR'] },
    loadComponent: () =>
      import('./features/organizador/evento-form.component').then((m) => m.EventoFormComponent),
  },
  { path: '**', redirectTo: '' },
];
