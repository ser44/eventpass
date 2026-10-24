export type Rol = 'ASISTENTE' | 'ORGANIZADOR' | 'ADMIN';

export interface Usuario {
  id: string;
  email: string;
  nombre: string;
  rol: Rol;
  activo: boolean;
  creadoEn: string;
}

export interface LoginResponse {
  token: string;
  usuario: Usuario;
}
