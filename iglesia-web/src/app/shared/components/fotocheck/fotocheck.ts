import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal } from '@angular/core';
import { iniciales } from '@/app/core/iglesias/models/iglesia';
import { IdentidadIglesiaService } from '@/app/core/iglesias/services/identidad-iglesia.service';
import { formatearCodigoAsistencia } from '@/app/shared/utils/codigo-asistencia';
import { generarQr } from '@/app/shared/utils/qr';

/** Lo que muestra un fotocheck: el miembro y su código de asistencia. */
export interface DatosFotocheck {
    readonly nombres: string;
    readonly apellidos: string;
    readonly nivel: string | null;
    readonly codigo: string;
    readonly contenidoQr: string;
}

/**
 * Fotocheck (presentacional): credencial horizontal del tamaño CR80 (85,6 × 54 mm) con la identidad de la iglesia, el
 * miembro y su QR. Se usa en la ficha y en la impresión en lote; la imagen que se descarga sigue el mismo diseño.
 */
@Component({
    selector: 'app-fotocheck',
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './fotocheck.html'
})
export class FotocheckComponent {
    private readonly identidad = inject(IdentidadIglesiaService);

    readonly datos = input.required<DatosFotocheck>();

    protected readonly qr = signal<string | null>(null);
    protected readonly nombreIglesia = computed(() => this.identidad.nombreComercial() ?? '');
    protected readonly logo = this.identidad.urlLogo;
    protected readonly monograma = computed(() => iniciales(this.nombreIglesia()));
    protected readonly codigoImpreso = computed(() => formatearCodigoAsistencia(this.datos().codigo));

    constructor() {
        effect(() => {
            const contenido = this.datos().contenidoQr;
            this.qr.set(null);
            generarQr(contenido).then(
                (url) => this.datos().contenidoQr === contenido && this.qr.set(url),
                () => this.qr.set(null)
            );
        });
    }
}
