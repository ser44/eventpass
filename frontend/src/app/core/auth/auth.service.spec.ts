import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from './auth.service';
import { Usuario } from '../models/usuario.model';

describe('AuthService', () => {
  let service: AuthService;
  let ctrl: HttpTestingController;

  const usuario: Usuario = {
    id: '1', email: 'a@test.dev', nombre: 'Ana', rol: 'ASISTENTE', activo: true, creadoEn: '2026-01-01T00:00:00Z',
  };

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    service = TestBed.inject(AuthService);
    ctrl = TestBed.inject(HttpTestingController);
  });

  afterEach(() => ctrl.verify());

  it('login guarda el token y el usuario', () => {
    service.login('a@test.dev', 'x').subscribe();
    ctrl.expectOne((r) => r.url.endsWith('/auth/login')).flush({ token: 'tok', usuario });

    expect(service.token).toBe('tok');
    expect(service.usuario()?.email).toBe('a@test.dev');
    expect(service.estaAutenticado()).toBeTrue();
  });

  it('logout limpia la sesión', () => {
    service.login('a@test.dev', 'x').subscribe();
    ctrl.expectOne((r) => r.url.endsWith('/auth/login')).flush({ token: 'tok', usuario });

    service.logout();
    expect(service.token).toBeNull();
    expect(service.usuario()).toBeNull();
    expect(service.estaAutenticado()).toBeFalse();
  });

  it('tieneAlgunRol responde según el rol del usuario', () => {
    service.login('a@test.dev', 'x').subscribe();
    ctrl.expectOne((r) => r.url.endsWith('/auth/login')).flush({ token: 'tok', usuario });

    expect(service.tieneAlgunRol(['ASISTENTE'])).toBeTrue();
    expect(service.tieneAlgunRol(['ADMIN', 'ORGANIZADOR'])).toBeFalse();
  });
});
