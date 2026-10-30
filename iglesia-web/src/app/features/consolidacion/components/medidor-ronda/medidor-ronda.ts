import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { MeterGroupModule } from 'primeng/metergroup';
import { porcentaje } from '../../models/consolidacion';

/** Cantidades de una ronda que muestra el medidor. */
export interface AvanceMedidor {
    readonly personas: number;
    readonly pendientes: number;
    readonly enContacto: number;
    readonly cerrados: number;
}

/**
 * Avance de una ronda en un `p-metergroup` (presentacional): cerrados, en contacto y pendientes, con su leyenda. Los
 * colores salen de los tokens del tema, así se ven bien en claro y en oscuro.
 */
@Component({
    selector: 'app-medidor-ronda',
    imports: [MeterGroupModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './medidor-ronda.html'
})
export class MedidorRondaComponent {
    readonly avance = input.required<AvanceMedidor>();
    /** Sin leyenda (p. ej. dentro de una fila de la tabla). */
    readonly compacto = input(false);

    protected readonly valores = computed(() => {
        const { personas, pendientes, enContacto, cerrados } = this.avance();
        return [
            { label: `Cerrados ${cerrados}`, value: porcentaje(cerrados, personas), color: 'var(--p-primary-500)' },
            { label: `En contacto ${enContacto}`, value: porcentaje(enContacto, personas), color: 'var(--p-primary-200)' },
            { label: `Pendientes ${pendientes}`, value: porcentaje(pendientes, personas), color: 'var(--p-surface-300)' }
        ];
    });

    protected readonly descripcion = computed(() => {
        const { personas, pendientes, enContacto, cerrados } = this.avance();
        return `${cerrados} cerrados, ${enContacto} en contacto y ${pendientes} pendientes de ${personas} personas`;
    });
}
