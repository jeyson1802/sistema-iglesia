import { ChangeDetectionStrategy, Component, computed, effect, input, signal } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { SkeletonModule } from 'primeng/skeleton';
import { CodigoAsistencia } from '@/app/features/miembros/models/miembro';
import { formatearCodigoAsistencia } from '@/app/shared/utils/codigo-asistencia';
import { generarQr } from '@/app/shared/utils/qr';

/**
 * «Mi código de asistencia» (presentacional): el QR personal en grande, el mismo del fotocheck, para mostrarlo en
 * la puerta del culto; se puede descargar o ver en pantalla completa. El QR va siempre en negro sobre blanco.
 */
@Component({
    selector: 'app-mi-codigo-qr',
    imports: [ButtonModule, DialogModule, SkeletonModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './mi-codigo-qr.html'
})
export class MiCodigoQrComponent {
    readonly codigo = input.required<CodigoAsistencia>();

    protected readonly qr = signal<string | null>(null);
    protected readonly pantallaCompleta = signal(false);
    protected readonly codigoLegible = computed(() => formatearCodigoAsistencia(this.codigo().codigo));

    constructor() {
        effect(() => {
            const contenido = this.codigo().contenidoQr;
            this.qr.set(null);
            generarQr(contenido).then(
                (url) => this.codigo().contenidoQr === contenido && this.qr.set(url),
                () => this.qr.set(null)
            );
        });
    }
}
