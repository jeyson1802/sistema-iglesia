import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { FILAS_POR_PAGINA, OPCIONES_FILAS_POR_PAGINA, PLANTILLA_REPORTE_PAGINA } from '@/app/shared/utils/paginacion';
import { CategoriaAporte } from '../../models/configuracion-aportes';

const FILAS_CARGA = 3;

/** Categorías de aporte en su orden (presentacional): subir, bajar, editar y desactivar o reactivar. */
@Component({
    selector: 'app-tabla-categorias',
    imports: [ButtonModule, SkeletonModule, TableModule, TagModule, TooltipModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './tabla-categorias.html'
})
export class TablaCategoriasComponent {
    protected readonly filasPorPagina = FILAS_POR_PAGINA;
    protected readonly opcionesPorPagina = OPCIONES_FILAS_POR_PAGINA;
    protected readonly plantillaReporte = PLANTILLA_REPORTE_PAGINA;

    readonly categorias = input.required<readonly CategoriaAporte[]>();
    readonly cargando = input(false);
    readonly ordenando = input(false);

    readonly subir = output<CategoriaAporte>();
    readonly bajar = output<CategoriaAporte>();
    readonly editar = output<CategoriaAporte>();
    readonly cambiarEstado = output<CategoriaAporte>();

    protected readonly filasCarga = Array.from({ length: FILAS_CARGA }, (_, indice) => indice);
    protected readonly filas = computed(() => [...this.categorias()]);
}
