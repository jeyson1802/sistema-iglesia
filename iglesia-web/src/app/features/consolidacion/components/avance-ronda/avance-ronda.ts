import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { ProgressBarModule } from 'primeng/progressbar';
import { TableModule } from 'primeng/table';
import { AvanceRonda, porcentaje } from '../../models/consolidacion';

/** Una etapa del embudo. */
interface Etapa {
    readonly etiqueta: string;
    readonly cantidad: number;
    /** Sobre el total de personas (largo de la barra). */
    readonly delTotal: number;
    /** Sobre la etapa anterior; `null` en la primera. */
    readonly deLaAnterior: number | null;
}

const HORAS_POR_DIA = 24;

/**
 * Avance de una ronda (presentacional): embudo Personas → Contactadas → Cerradas → Volvieron a asistir con el
 * porcentaje entre etapas, resultados de la iteración, avance por consolidador y tiempo medio hasta el primer contacto.
 * Todo con barras de texto legible (sin depender del color) y tablas accesibles.
 */
@Component({
    selector: 'app-avance-ronda',
    imports: [ProgressBarModule, TableModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './avance-ronda.html'
})
export class AvanceRondaComponent {
    readonly avance = input.required<AvanceRonda>();

    protected readonly etapas = computed<readonly Etapa[]>(() => {
        const { personas, contactadas, cerrados, volvieronAAsistir } = this.avance();
        const valores = [
            { etiqueta: 'Personas', cantidad: personas },
            { etiqueta: 'Contactadas', cantidad: contactadas },
            { etiqueta: 'Cerradas', cantidad: cerrados },
            { etiqueta: 'Volvieron a asistir', cantidad: volvieronAAsistir }
        ];
        return valores.map((etapa, indice) => ({ ...etapa, delTotal: porcentaje(etapa.cantidad, personas), deLaAnterior: indice === 0 ? null : porcentaje(etapa.cantidad, valores[indice - 1].cantidad) }));
    });
    protected readonly resultados = computed(() => {
        const total = this.avance().porResultado.reduce((suma, { cantidad }) => suma + cantidad, 0);
        return this.avance().porResultado.map(({ resultado, cantidad }) => ({ nombre: resultado?.nombre ?? 'No concluido', cantidad, porcentaje: porcentaje(cantidad, total) }));
    });
    protected readonly consolidadores = computed(() => this.avance().porConsolidador.map((fila) => ({ ...fila, nombre: fila.consolidador?.nombre ?? 'Sin asignar', avance: porcentaje(fila.contactados, fila.asignados) })));
    protected readonly tiempoPrimerContacto = computed(() => {
        const horas = this.avance().horasHastaPrimerContacto;
        if (horas === null) {
            return null;
        }
        if (horas < HORAS_POR_DIA) {
            return `${Math.max(1, Math.round(horas))} h`;
        }
        const dias = Math.round(horas / HORAS_POR_DIA);
        return dias === 1 ? '1 día' : `${dias} días`;
    });
}
