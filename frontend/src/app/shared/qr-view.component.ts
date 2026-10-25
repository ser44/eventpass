import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { QRCodeModule } from 'angularx-qrcode';

/** Muestra el QR de un código de entrada sobre fondo blanco, con el código debajo. */
@Component({
  selector: 'app-qr-view',
  standalone: true,
  imports: [QRCodeModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="qr">
      <qrcode [qrdata]="codigo()" [width]="ancho()" errorCorrectionLevel="M" [margin]="2"></qrcode>
      <code class="codigo">{{ codigo() }}</code>
    </div>
  `,
  styles: [`
    .qr { display: flex; flex-direction: column; align-items: center; gap: 8px;
          background: #fff; padding: 12px; border-radius: 8px; }
    .codigo { font-size: 14px; letter-spacing: 1px; color: #000; }
  `],
})
export class QrViewComponent {
  readonly codigo = input.required<string>();
  readonly ancho = input(220);
}
