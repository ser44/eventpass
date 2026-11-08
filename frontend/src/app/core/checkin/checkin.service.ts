import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { CheckInResultado, Stats } from '../models/checkin.model';
import { SILENCIAR_ERROR } from '../auth/error.interceptor';

@Injectable({ providedIn: 'root' })
export class CheckinService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/events`;

  checkIn(eventoId: string, codigo: string): Observable<CheckInResultado> {
    // Silenciamos el snackbar: el resultado se muestra a pantalla completa.
    const context = new HttpContext().set(SILENCIAR_ERROR, true);
    return this.http.post<CheckInResultado>(`${this.base}/${eventoId}/check-ins`, { codigo }, { context });
  }

  stats(eventoId: string): Observable<Stats> {
    return this.http.get<Stats>(`${this.base}/${eventoId}/stats`);
  }
}
