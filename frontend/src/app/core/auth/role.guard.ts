import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';
import { Rol } from '../models/usuario.model';

/**
 * Protege rutas por autenticación y rol. Declarar los roles permitidos en
 * `data: { roles: ['ORGANIZADOR'] }`. Sin roles, solo exige estar logueado.
 */
export const roleGuard: CanActivateFn = (route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const roles = (route.data?.['roles'] as Rol[] | undefined) ?? [];

  if (!auth.estaAutenticado()) {
    return router.createUrlTree(['/login'], { queryParams: { redirect: state.url } });
  }
  if (roles.length > 0 && !auth.tieneAlgunRol(roles)) {
    return router.createUrlTree(['/']);
  }
  return true;
};
