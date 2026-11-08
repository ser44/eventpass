import {
  ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal,
} from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { ZXingScannerModule } from '@zxing/ngx-scanner';
import { BarcodeFormat } from '@zxing/library';
import { CheckinService } from '../../core/checkin/checkin.service';

type Estado = 'escaneando' | 'validando' | 'ok' | 'error';

@Component({
  selector: 'app-check-in',
  standalone: true,
  imports: [
    RouterLink, FormsModule, ZXingScannerModule,
    MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './check-in.component.html',
  styleUrl: './check-in.component.scss',
})
export class CheckInComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly checkin = inject(CheckinService);
  private readonly destroyRef = inject(DestroyRef);

  readonly estado = signal<Estado>('escaneando');
  readonly titular = signal('');
  readonly mensajeError = signal('');
  readonly camaras = signal<MediaDeviceInfo[]>([]);
  readonly camaraActual = signal<MediaDeviceInfo | undefined>(undefined);

  readonly formats = [BarcodeFormat.QR_CODE];
  codigoManual = '';

  private eventoId = '';
  private ultimoCodigo = '';
  private ultimoMs = 0;
  private timer: ReturnType<typeof setTimeout> | undefined;

  ngOnInit(): void {
    this.eventoId = this.route.snapshot.paramMap.get('id') ?? '';
    this.destroyRef.onDestroy(() => clearTimeout(this.timer));
  }

  onCamaras(dispositivos: MediaDeviceInfo[]): void {
    this.camaras.set(dispositivos);
    const trasera = dispositivos.find((d) => /back|rear|trasera|environment/i.test(d.label));
    this.camaraActual.set(trasera ?? dispositivos[dispositivos.length - 1]);
  }

  cambiarCamara(deviceId: string): void {
    this.camaraActual.set(this.camaras().find((d) => d.deviceId === deviceId));
  }

  onScan(codigo: string): void {
    this.validar(codigo);
  }

  validarManual(): void {
    this.validar(this.codigoManual);
    this.codigoManual = '';
  }

  private validar(codigo: string): void {
    codigo = (codigo ?? '').trim();
    if (!codigo || this.estado() === 'validando') {
      return;
    }
    const ahora = Date.now();
    // Evita revalidar el mismo QR por lecturas repetidas en ~1,5 s.
    if (codigo === this.ultimoCodigo && ahora - this.ultimoMs < 1500) {
      return;
    }
    if (this.estado() === 'ok' || this.estado() === 'error') {
      return;
    }
    this.ultimoCodigo = codigo;
    this.ultimoMs = ahora;
    this.estado.set('validando');

    this.checkin.checkIn(this.eventoId, codigo).subscribe({
      next: (r) => {
        this.titular.set(r.titular ?? '');
        this.estado.set('ok');
        this.vibrar([120]);
        this.autoReset();
      },
      error: (e: HttpErrorResponse) => {
        this.mensajeError.set(this.mensaje(e));
        this.estado.set('error');
        this.vibrar([80, 40, 80]);
        this.autoReset();
      },
    });
  }

  private autoReset(): void {
    clearTimeout(this.timer);
    this.timer = setTimeout(() => {
      this.estado.set('escaneando');
      this.ultimoCodigo = '';
    }, 2500);
  }

  private vibrar(patron: number[]): void {
    if (typeof navigator !== 'undefined' && navigator.vibrate) {
      navigator.vibrate(patron);
    }
  }

  private mensaje(e: HttpErrorResponse): string {
    const code = e.error?.code as string | undefined;
    switch (code) {
      case 'ENTRADA_YA_USADA': {
        const h = e.error?.usadaEn
          ? new Date(e.error.usadaEn).toLocaleTimeString('es-AR', { hour: '2-digit', minute: '2-digit' })
          : '';
        return `Ya utilizada a las ${h}`;
      }
      case 'ENTRADA_INEXISTENTE':
        return 'Entrada inexistente';
      case 'ENTRADA_ANULADA':
        return 'Entrada anulada';
      case 'ENTRADA_OTRO_EVENTO':
        return 'Entrada de otro evento';
      default:
        return e.status === 0 ? 'Sin conexión' : 'No se pudo validar';
    }
  }
}
