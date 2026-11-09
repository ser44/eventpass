export interface Banner {
  bg: string;
  fg: string;
  sub: string;
}

/** Paletas de banner por evento (el documento usa colores por evento). */
const BANNERS: Banner[] = [
  { bg: '#5b2dcb', fg: '#ffffff', sub: 'rgba(255,255,255,.75)' },
  { bg: '#17151f', fg: '#ffffff', sub: 'rgba(255,255,255,.7)' },
  { bg: '#fbe7c6', fg: '#5b3200', sub: 'rgba(91,50,0,.72)' },
  { bg: '#dcebff', fg: '#0b3a75', sub: 'rgba(11,58,117,.72)' },
  { bg: '#e7f6ec', fg: '#0f6b37', sub: 'rgba(15,107,55,.72)' },
  { bg: '#ece6fb', fg: '#3f1d96', sub: 'rgba(63,29,150,.72)' },
];

const MESES = ['ENE', 'FEB', 'MAR', 'ABR', 'MAY', 'JUN', 'JUL', 'AGO', 'SEP', 'OCT', 'NOV', 'DIC'];

/** Color de banner estable para un evento (hash de su id). */
export function bannerPara(id: string): Banner {
  let h = 0;
  for (let i = 0; i < id.length; i++) {
    h = (h * 31 + id.charCodeAt(i)) >>> 0;
  }
  return BANNERS[h % BANNERS.length];
}

export function mesCorto(fechaIso: string): string {
  return MESES[new Date(fechaIso).getMonth()];
}

export function diaDe(fechaIso: string): number {
  return new Date(fechaIso).getDate();
}

export function horaDe(fechaIso: string): string {
  return new Date(fechaIso).toLocaleTimeString('es-AR', { hour: '2-digit', minute: '2-digit', hour12: false });
}
