import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Entrada, Reserva, ReservaConfirmada } from '../models/reserva.model';

@Injectable({ providedIn: 'root' })
export class ReservaService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiUrl;

  reservar(eventoId: string, cantidad: number, idempotencyKey: string): Observable<Reserva> {
    const headers = new HttpHeaders({ 'Idempotency-Key': idempotencyKey });
    return this.http.post<Reserva>(`${this.api}/events/${eventoId}/reservations`, { cantidad }, { headers });
  }

  detalle(id: string): Observable<Reserva> {
    return this.http.get<Reserva>(`${this.api}/reservations/${id}`);
  }

  confirmar(id: string): Observable<ReservaConfirmada> {
    return this.http.post<ReservaConfirmada>(`${this.api}/reservations/${id}/confirm`, {});
  }

  cancelar(id: string): Observable<void> {
    return this.http.delete<void>(`${this.api}/reservations/${id}`);
  }

  misEntradas(): Observable<Entrada[]> {
    return this.http.get<Entrada[]>(`${this.api}/me/tickets`);
  }
}
