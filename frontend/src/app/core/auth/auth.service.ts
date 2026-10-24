import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { LoginResponse, Rol, Usuario } from '../models/usuario.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  private readonly TOKEN_KEY = 'eventpass_token';
  private readonly USER_KEY = 'eventpass_user';

  /** Usuario actual (null si no hay sesión). Fuente de verdad reactiva para la UI. */
  readonly usuario = signal<Usuario | null>(this.restaurarUsuario());
  readonly estaAutenticado = computed(() => this.usuario() !== null);

  get token(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  login(email: string, password: string): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${environment.apiUrl}/auth/login`, { email, password })
      .pipe(tap((r) => this.guardarSesion(r)));
  }

  register(email: string, nombre: string, password: string): Observable<Usuario> {
    return this.http.post<Usuario>(`${environment.apiUrl}/auth/register`, { email, nombre, password });
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    this.usuario.set(null);
    this.router.navigate(['/login']);
  }

  tieneAlgunRol(roles: Rol[]): boolean {
    const u = this.usuario();
    return u !== null && roles.includes(u.rol);
  }

  private guardarSesion(r: LoginResponse): void {
    localStorage.setItem(this.TOKEN_KEY, r.token);
    localStorage.setItem(this.USER_KEY, JSON.stringify(r.usuario));
    this.usuario.set(r.usuario);
  }

  private restaurarUsuario(): Usuario | null {
    const raw = localStorage.getItem(this.USER_KEY);
    return raw ? (JSON.parse(raw) as Usuario) : null;
  }
}
