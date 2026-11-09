import { TestBed } from '@angular/core/testing';
import { CountdownComponent } from './countdown.component';

describe('CountdownComponent', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [CountdownComponent] });
  });

  it('con fecha pasada muestra "Vencida" y emite expirado', () => {
    const fixture = TestBed.createComponent(CountdownComponent);
    let emitido = false;
    fixture.componentInstance.expirado.subscribe(() => (emitido = true));
    fixture.componentRef.setInput('expiraEn', new Date(Date.now() - 1000).toISOString());
    fixture.detectChanges();

    expect(fixture.componentInstance.restante()).toBe(0);
    expect(fixture.componentInstance.etiqueta()).toBe('Vencida');
    expect(emitido).toBeTrue();
  });

  it('con fecha futura muestra el tiempo en formato mm:ss', () => {
    const fixture = TestBed.createComponent(CountdownComponent);
    fixture.componentRef.setInput('expiraEn', new Date(Date.now() + 65000).toISOString());
    fixture.detectChanges();

    expect(fixture.componentInstance.restante()).toBeGreaterThan(0);
    expect(fixture.componentInstance.etiqueta()).toMatch(/^\d{2}:\d{2}$/);
  });
});
