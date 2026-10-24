export type EstadoEvento = 'BORRADOR' | 'PUBLICADO' | 'CANCELADO' | 'FINALIZADO';

export interface EventoResumen {
  id: string;
  titulo: string;
  lugar: string;
  fechaInicio: string;
  cupoTotal: number;
  cupoDisponible: number;
  estado: EstadoEvento;
  imagenUrl?: string | null;
}

export interface Evento extends EventoResumen {
  organizadorId: string;
  descripcion?: string | null;
  version: number;
  creadoEn: string;
  actualizadoEn: string;
}

export interface Pagina<T> {
  contenido: T[];
  pagina: number;
  tamanio: number;
  totalElementos: number;
  totalPaginas: number;
}

export interface CrearEvento {
  titulo: string;
  descripcion?: string;
  lugar: string;
  fechaInicio: string;
  cupoTotal: number;
  imagenUrl?: string;
}

export interface ActualizarEvento extends CrearEvento {
  version: number;
}
