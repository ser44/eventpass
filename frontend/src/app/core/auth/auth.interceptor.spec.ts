import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let ctrl: HttpTestingController;

  function configurar(): void {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        provideRouter([]),
      ],
    });
    http = TestBed.inject(HttpClient);
    ctrl = TestBed.inject(HttpTestingController);
  }

  afterEach(() => {
    localStorage.clear();
    ctrl.verify();
  });

  it('agrega el header Authorization cuando hay token', () => {
    localStorage.setItem('eventpass_token', 'tok');
    configurar();
    http.get('/x').subscribe();
    const req = ctrl.expectOne('/x');
    expect(req.request.headers.get('Authorization')).toBe('Bearer tok');
  });

  it('no agrega Authorization cuando no hay token', () => {
    localStorage.clear();
    configurar();
    http.get('/x').subscribe();
    const req = ctrl.expectOne('/x');
    expect(req.request.headers.has('Authorization')).toBeFalse();
  });
});
