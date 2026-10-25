export type EstadoReserva = 'PENDIENTE' | 'CONFIRMADA' | 'EXPIRADA' | 'CANCELADA';
export type EstadoEntrada = 'VALIDA' | 'USADA' | 'ANULADA';

export interface Reserva {
  id: string;
  eventoId: string;
  eventoTitulo: string;
  cantidad: number;
  estado: EstadoReserva;
  expiraEn: string;
  creadaEn: string;
}

export interface Entrada {
  id: string;
  codigo: string;
  estado: EstadoEntrada;
  usadaEn?: string | null;
  reservaId: string;
  eventoId: string;
  eventoTitulo: string;
  eventoFecha: string;
  eventoLugar: string;
}

export interface ReservaConfirmada {
  id: string;
  eventoId: string;
  eventoTitulo: string;
  estado: EstadoReserva;
  entradas: Entrada[];
}
