import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';
import { roleGuard } from './role.guard';
import { AuthService } from './auth.service';

describe('roleGuard', () => {
  let auth: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;

  const estado = { url: '/organizador' } as RouterStateSnapshot;
  const ruta = (roles?: string[]) => ({ data: roles ? { roles } : {} }) as unknown as ActivatedRouteSnapshot;

  beforeEach(() => {
    auth = jasmine.createSpyObj('AuthService', ['estaAutenticado', 'tieneAlgunRol']);
    router = jasmine.createSpyObj('Router', ['createUrlTree']);
    router.createUrlTree.and.returnValue({} as any);
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: auth },
        { provide: Router, useValue: router },
      ],
    });
  });

  it('redirige a /login si no está autenticado', () => {
    auth.estaAutenticado.and.returnValue(false);
    TestBed.runInInjectionContext(() => roleGuard(ruta(), estado));
    expect(router.createUrlTree).toHaveBeenCalledWith(['/login'], jasmine.anything());
  });

  it('redirige al inicio si el rol no está permitido', () => {
    auth.estaAutenticado.and.returnValue(true);
    auth.tieneAlgunRol.and.returnValue(false);
    TestBed.runInInjectionContext(() => roleGuard(ruta(['ORGANIZADOR']), estado));
    expect(router.createUrlTree).toHaveBeenCalledWith(['/']);
  });

  it('permite el acceso si el rol es correcto', () => {
    auth.estaAutenticado.and.returnValue(true);
    auth.tieneAlgunRol.and.returnValue(true);
    const resultado = TestBed.runInInjectionContext(() => roleGuard(ruta(['ORGANIZADOR']), estado));
    expect(resultado).toBeTrue();
  });
});
