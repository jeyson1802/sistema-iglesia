import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { FILAS_POR_PAGINA, OPCIONES_FILAS_POR_PAGINA, PLANTILLA_REPORTE_PAGINA } from '@/app/shared/utils/paginacion';
import { ResultadoConsolidacion } from '../../models/consolidacion';

/** Filas de relleno que se muestran como skeleton mientras se cargan los resultados. */
const FILAS_CARGA = 4;

/** Tabla de resultados de consolidación en el orden en que se ofrecen al cerrar un caso (presentacional): subir, bajar, renombrar y desactivar o reactivar. */
@Component({
    selector: 'app-tabla-resultados',
    imports: [ButtonModule, SkeletonModule, TableModule, TagModule, TooltipModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './tabla-resultados.html'
})
export class TablaResultadosComponent {
    protected readonly filasPorPagina = FILAS_POR_PAGINA;
    protected readonly opcionesPorPagina = OPCIONES_FILAS_POR_PAGINA;
    protected readonly plantillaReporte = PLANTILLA_REPORTE_PAGINA;

    readonly resultados = input.required<readonly ResultadoConsolidacion[]>();
    readonly cargando = input(false);
    /** Mientras se guarda un nuevo orden no se puede volver a mover. */
    readonly ordenando = input(false);

    readonly subir = output<ResultadoConsolidacion>();
    readonly bajar = output<ResultadoConsolidacion>();
    readonly editar = output<ResultadoConsolidacion>();
    readonly cambiarEstado = output<ResultadoConsolidacion>();

    protected readonly filasCarga = Array.from({ length: FILAS_CARGA }, (_, indice) => indice);
    /** `p-table` espera un arreglo mutable. */
    protected readonly filas = computed(() => [...this.resultados()]);
}
