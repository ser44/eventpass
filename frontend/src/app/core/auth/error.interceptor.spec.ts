import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { errorInterceptor } from './error.interceptor';

describe('errorInterceptor (CP07)', () => {
  let http: HttpClient;
  let ctrl: HttpTestingController;
  let snack: jasmine.SpyObj<MatSnackBar>;

  beforeEach(() => {
    localStorage.clear();
    snack = jasmine.createSpyObj('MatSnackBar', ['open']);
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: MatSnackBar, useValue: snack },
      ],
    });
    http = TestBed.inject(HttpClient);
    ctrl = TestBed.inject(HttpTestingController);
  });

  afterEach(() => ctrl.verify());

  it('CP07: ante 409 CUPO_AGOTADO muestra "No quedan entradas…"', () => {
    http.get('/api/v1/test').subscribe({ next: () => {}, error: () => {} });
    ctrl.expectOne('/api/v1/test').flush({ code: 'CUPO_AGOTADO' }, { status: 409, statusText: 'Conflict' });

    expect(snack.open).toHaveBeenCalled();
    const mensaje = snack.open.calls.mostRecent().args[0] as string;
    expect(mensaje).toContain('No quedan entradas');
  });

  it('ante error de red (status 0) muestra "Sin conexión"', () => {
    http.get('/api/v1/test').subscribe({ next: () => {}, error: () => {} });
    ctrl.expectOne('/api/v1/test').error(new ProgressEvent('error'), { status: 0, statusText: '' });

    const mensaje = snack.open.calls.mostRecent().args[0] as string;
    expect(mensaje).toContain('Sin conexión');
  });
});
