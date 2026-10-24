// La API se consume por ruta relativa: nginx (o el proxy de ng serve) la redirige.
// Así el navegador ve un único origen y no hace falta CORS.
export const environment = {
  production: false,
  apiUrl: '/api/v1',
};
