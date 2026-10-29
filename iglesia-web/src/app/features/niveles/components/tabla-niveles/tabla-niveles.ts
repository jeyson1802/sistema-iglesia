import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { FILAS_POR_PAGINA, OPCIONES_FILAS_POR_PAGINA, PLANTILLA_REPORTE_PAGINA } from '@/app/shared/utils/paginacion';
import { Nivel } from '../../models/nivel';

/** Filas de relleno que se muestran como skeleton mientras se cargan los niveles. */
const FILAS_CARGA = 4;

/** Tabla de niveles en su orden jerárquico (presentacional): subir, bajar, renombrar y desactivar o reactivar. */
@Component({
    selector: 'app-tabla-niveles',
    imports: [ButtonModule, SkeletonModule, TableModule, TagModule, TooltipModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './tabla-niveles.html'
})
export class TablaNivelesComponent {
    protected readonly filasPorPagina = FILAS_POR_PAGINA;
    protected readonly opcionesPorPagina = OPCIONES_FILAS_POR_PAGINA;
    protected readonly plantillaReporte = PLANTILLA_REPORTE_PAGINA;

    readonly niveles = input.required<readonly Nivel[]>();
    readonly cargando = input(false);
    /** Mientras se guarda un nuevo orden no se puede volver a mover. */
    readonly ordenando = input(false);

    readonly subir = output<Nivel>();
    readonly bajar = output<Nivel>();
    readonly editar = output<Nivel>();
    readonly cambiarEstado = output<Nivel>();

    protected readonly filasCarga = Array.from({ length: FILAS_CARGA }, (_, indice) => indice);
    /** `p-table` espera un arreglo mutable. */
    protected readonly filas = computed(() => [...this.niveles()]);
}
