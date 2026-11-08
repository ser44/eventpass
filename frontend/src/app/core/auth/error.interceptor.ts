import { HttpContextToken, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

/** Si está en true en el contexto de la request, el interceptor NO muestra snackbar
 * (lo usa el check-in, que muestra el error a pantalla completa). El 401 se maneja igual. */
export const SILENCIAR_ERROR = new HttpContextToken<boolean>(() => false);

/** Traduce `code` de Problem Details a mensajes en español. */
function mensajeDeError(err: HttpErrorResponse): string {
  if (err.status === 0) {
    return 'Sin conexión con el servidor.';
  }
  const code = err.error?.code as string | undefined;
  const mapa: Record<string, string> = {
    CREDENCIALES_INVALIDAS: 'Email o contraseña incorrectos.',
    EMAIL_DUPLICADO: 'Ya existe una cuenta con ese email.',
    CUPO_AGOTADO: 'No quedan entradas suficientes.',
    RESERVA_EXPIRADA: 'Tu reserva venció. Volvé a intentarlo.',
    CANCELACION_FUERA_DE_PLAZO: 'Ya no se puede cancelar (menos de 48 h antes).',
    ENTRADA_YA_USADA: 'Esa entrada ya fue utilizada.',
    ENTRADA_INEXISTENTE: 'Entrada inexistente.',
    VALIDACION: 'Revisá los datos ingresados.',
  };
  return (code && mapa[code]) || (err.error?.detail as string) || 'Ocurrió un error inesperado.';
}

/**
 * Centraliza el manejo de errores HTTP: en 401 (salvo el propio login) cierra
 * sesión y redirige al login; en el resto muestra un MatSnackBar.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const snack = inject(MatSnackBar);
  const auth = inject(AuthService);
  const router = inject(Router);

  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      const esLogin = req.url.includes('/auth/login');
      if (err.status === 401 && !esLogin) {
        auth.logout();
        router.navigate(['/login'], { queryParams: { redirect: router.url } });
      } else if (!req.context.get(SILENCIAR_ERROR)) {
        snack.open(mensajeDeError(err), 'Cerrar', { duration: 5000 });
      }
      return throwError(() => err);
    }),
  );
};
