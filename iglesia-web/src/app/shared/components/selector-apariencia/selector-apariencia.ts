import { ChangeDetectionStrategy, Component, inject, input, model } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SelectButtonModule } from 'primeng/selectbutton';
import { Apariencia, NombreTipografia, TIPOGRAFIAS } from '@/app/core/apariencia/models/apariencia';
import { MUESTRAS_PRIMARIAS, MUESTRAS_SUPERFICIE } from '@/app/core/apariencia/models/muestras';
import { NombrePreset, NOMBRES_PRESETS } from '@/app/core/apariencia/models/paletas';
import { AparienciaService } from '@/app/core/apariencia/services/apariencia.service';

/**
 * Elección de la apariencia de una iglesia (presentacional): preset, color primario, superficie y tipografía de
 * marca, con una tarjeta por fuente que muestra el nombre comercial escrito con ella. Cada cambio se emite al
 * momento para que el contenedor lo aplique en vivo.
 */
@Component({
    selector: 'app-selector-apariencia',
    imports: [FormsModule, SelectButtonModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './selector-apariencia.html',
    host: { class: 'flex flex-col gap-6' }
})
export class SelectorAparienciaComponent {
    readonly apariencia = model.required<Apariencia>();
    /** Nombre que se muestra en las tarjetas de tipografía. */
    readonly nombreComercial = input('Mi iglesia');
    readonly deshabilitado = input(false);

    // `p-selectbutton` espera un arreglo mutable.
    protected readonly presets = [...NOMBRES_PRESETS];
    protected readonly primarias = MUESTRAS_PRIMARIAS;
    protected readonly superficies = MUESTRAS_SUPERFICIE;
    protected readonly tipografias = TIPOGRAFIAS;

    constructor() {
        // La vista previa necesita las cinco fuentes; el resto de la aplicación carga solo la elegida.
        inject(AparienciaService).cargarFuentes(TIPOGRAFIAS.map(({ nombre }) => nombre));
    }

    protected elegirPreset(preset: NombrePreset): void {
        this.cambiar({ preset });
    }

    protected elegirPrimario(colorPrimario: string): void {
        this.cambiar({ colorPrimario });
    }

    /** Elegir la superficie ya elegida la quita: se usa la propia del preset. */
    protected elegirSuperficie(superficie: string): void {
        this.cambiar({ colorSuperficie: this.apariencia().colorSuperficie === superficie ? null : superficie });
    }

    protected elegirTipografia(tipografia: NombreTipografia): void {
        this.cambiar({ tipografia });
    }

    private cambiar(cambio: Partial<Apariencia>): void {
        if (!this.deshabilitado()) {
            this.apariencia.update((actual) => ({ ...actual, ...cambio }));
        }
    }
}
