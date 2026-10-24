import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ActualizarEvento, CrearEvento, Evento, EventoResumen, Pagina } from '../models/evento.model';

export interface FiltroCatalogo {
  q?: string;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}

@Injectable({ providedIn: 'root' })
export class EventoService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/events`;

  catalogo(filtro: FiltroCatalogo = {}): Observable<Pagina<EventoResumen>> {
    let params = new HttpParams();
    if (filtro.q) params = params.set('q', filtro.q);
    if (filtro.from) params = params.set('from', filtro.from);
    if (filtro.to) params = params.set('to', filtro.to);
    params = params.set('page', filtro.page ?? 0).set('size', filtro.size ?? 12);
    return this.http.get<Pagina<EventoResumen>>(this.base, { params });
  }

  detalle(id: string): Observable<Evento> {
    return this.http.get<Evento>(`${this.base}/${id}`);
  }

  crear(body: CrearEvento): Observable<Evento> {
    return this.http.post<Evento>(this.base, body);
  }

  actualizar(id: string, body: ActualizarEvento): Observable<Evento> {
    return this.http.put<Evento>(`${this.base}/${id}`, body);
  }

  publicar(id: string): Observable<Evento> {
    return this.http.post<Evento>(`${this.base}/${id}/publish`, {});
  }

  cancelar(id: string): Observable<Evento> {
    return this.http.post<Evento>(`${this.base}/${id}/cancel`, {});
  }

  misEventos(): Observable<EventoResumen[]> {
    return this.http.get<EventoResumen[]>(`${environment.apiUrl}/me/events`);
  }
}
