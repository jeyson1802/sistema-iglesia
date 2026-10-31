import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { TableModule } from 'primeng/table';
import { GraficoDistribucionComponent } from '@/app/shared/components/grafico-distribucion/grafico-distribucion';
import { GraficoEvolucionComponent, SerieGrafico } from '@/app/shared/components/grafico-evolucion/grafico-evolucion';
import { FILAS_POR_PAGINA, OPCIONES_FILAS_POR_PAGINA, PLANTILLA_REPORTE_PAGINA } from '@/app/shared/utils/paginacion';
import { EstadisticasSistema, etiquetaEje, etiquetaPunto, formatoNumero } from '../../models/estadisticas';
import { BloqueEstadisticoComponent } from '../bloque-estadistico/bloque-estadistico';
import { TarjetaIndicadorComponent } from '../tarjeta-indicador/tarjeta-indicador';

/**
 * Bloque «Crecimiento» del panorama del sistema (presentacional): iglesias nuevas, altas y bajas de miembros en
 * todas las iglesias, actividad por módulo, iglesias por tamaño, miembros por condición y las iglesias con más
 * miembros. Solo agregados: ningún dato de personas ni de aportes.
 */
@Component({
    selector: 'app-bloque-sistema',
    imports: [TableModule, BloqueEstadisticoComponent, TarjetaIndicadorComponent, GraficoEvolucionComponent, GraficoDistribucionComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './bloque-sistema.html'
})
export class BloqueSistemaComponent {
    readonly datos = input<EstadisticasSistema | null>(null);
    readonly cargando = input(false);
    readonly error = input(false);
    readonly reintentar = output<void>();

    protected readonly numero = formatoNumero;
    protected readonly eje = etiquetaEje;
    protected readonly mayores = computed(() => [...(this.datos()?.mayores ?? [])]);
    protected readonly filasPorPagina = FILAS_POR_PAGINA;
    protected readonly opcionesPorPagina = OPCIONES_FILAS_POR_PAGINA;
    protected readonly plantillaReporte = PLANTILLA_REPORTE_PAGINA;
    protected readonly etiquetas = computed(() => {
        const datos = this.datos();
        return datos ? datos.serie.map(({ inicio }) => etiquetaPunto(inicio, datos.periodo.granularidad)) : [];
    });
    protected readonly seriesMiembros = computed<readonly SerieGrafico[]>(() => {
        const serie = this.datos()?.serie ?? [];
        return [
            { etiqueta: 'Altas', valores: serie.map(({ altas }) => altas), tono: 'exito' },
            { etiqueta: 'Bajas', valores: serie.map(({ bajas }) => bajas), tono: 'peligro' },
            { etiqueta: 'Iglesias nuevas', valores: serie.map(({ iglesiasNuevas }) => iglesiasNuevas), tono: 'suave' }
        ];
    });
    protected readonly seriesModulos = computed<readonly SerieGrafico[]>(() => {
        const serie = this.datos()?.serie ?? [];
        return [
            { etiqueta: 'Miembros registrados', valores: serie.map(({ miembrosRegistrados }) => miembrosRegistrados), tono: 'primario' },
            { etiqueta: 'Asistencias marcadas', valores: serie.map(({ asistenciasMarcadas }) => asistenciasMarcadas), tono: 'exito' },
            { etiqueta: 'Contactos de consolidación', valores: serie.map(({ contactosConsolidacion }) => contactosConsolidacion), tono: 'suave' }
        ];
    });
}
