import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { FILAS_POR_PAGINA, OPCIONES_FILAS_POR_PAGINA, PLANTILLA_REPORTE_PAGINA } from '@/app/shared/utils/paginacion';
import { GraficoEvolucionComponent, SerieGrafico } from '@/app/shared/components/grafico-evolucion/grafico-evolucion';
import { ResumenSerie, resumenSerie, semanaCorta, ServiciosSeguimiento } from '../../models/seguimiento';

interface FilaResumen extends ResumenSerie {
    readonly clave: string;
    readonly servicio: string;
    readonly esPorcentaje: boolean;
}

/**
 * Evolución de los servicios de la iglesia (presentacional): asistentes de cada culto y personas distintas en los días
 * de culto, porcentaje de asistencia de los grupos y una tabla con el resumen de las semanas cerradas.
 */
@Component({
    selector: 'app-servicios-seguimiento',
    imports: [TableModule, TagModule, GraficoEvolucionComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './servicios-seguimiento.html'
})
export class ServiciosSeguimientoComponent {
    readonly servicios = input.required<ServiciosSeguimiento>();
    readonly nombreGrupos = input('Grupos');

    protected readonly filasPorPagina = FILAS_POR_PAGINA;
    protected readonly opcionesPorPagina = OPCIONES_FILAS_POR_PAGINA;
    protected readonly plantillaReporte = PLANTILLA_REPORTE_PAGINA;
    protected readonly etiquetas = computed(() => this.servicios().semanas.map(semanaCorta));
    protected readonly seriesCultos = computed<readonly SerieGrafico[]>(() => [
        { etiqueta: 'Personas en los cultos', valores: this.servicios().personasEnCultos, tono: 'primario' },
        ...this.servicios().cultos.map((culto) => ({ etiqueta: culto.nombre, valores: culto.asistentes, tono: 'suave' as const }))
    ]);
    protected readonly seriesGrupos = computed<readonly SerieGrafico[]>(() => [{ etiqueta: `Asistencia en ${this.nombreGrupos().toLowerCase()}`, valores: this.servicios().porcentajeGrupos, tono: 'primario' }]);
    protected readonly filas = computed<FilaResumen[]>(() => {
        const { cultos, personasEnCultos, presentesEnGrupos, porcentajeGrupos } = this.servicios();
        const grupos = this.nombreGrupos().toLowerCase();
        return [
            { clave: 'personas', servicio: 'Personas distintas en los cultos', esPorcentaje: false, ...resumenSerie(personasEnCultos) },
            ...cultos.map((culto) => ({ clave: `c${culto.cultoProgramadoId}`, servicio: culto.nombre, esPorcentaje: false, ...resumenSerie(culto.asistentes) })),
            { clave: 'presentes', servicio: `Asistencias en ${grupos}`, esPorcentaje: false, ...resumenSerie(presentesEnGrupos) },
            { clave: 'porcentaje', servicio: `Porcentaje de asistencia en ${grupos}`, esPorcentaje: true, ...resumenSerie(porcentajeGrupos, true) }
        ];
    });

    protected valor(valor: number | null, esPorcentaje: boolean): string {
        return valor === null ? '—' : esPorcentaje ? `${valor} %` : `${valor}`;
    }

    protected variacion({ variacion, esPorcentaje }: FilaResumen): string {
        if (variacion === null) {
            return '—';
        }
        const signo = variacion > 0 ? '+' : '';
        return esPorcentaje ? `${signo}${variacion} pts` : `${signo}${variacion} %`;
    }

    protected severidadVariacion({ variacion }: FilaResumen): 'success' | 'danger' | 'secondary' {
        return variacion === null || variacion === 0 ? 'secondary' : variacion > 0 ? 'success' : 'danger';
    }
}
