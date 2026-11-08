export interface CheckInResultado {
  resultado: string;
  titular: string;
  entradaId: string;
}

export interface Stats {
  cupoTotal: number;
  disponibles: number;
  retenidas: number;
  vendidas: number;
  canceladas: number;
  ingresadas: number;
}
