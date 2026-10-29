import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { PaginatorModule, PaginatorState } from 'primeng/paginator';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { FILAS_POR_PAGINA, OPCIONES_FILAS_POR_PAGINA, PaginaPedida, paginaPedida, PLANTILLA_REPORTE_PAGINA } from '@/app/shared/utils/paginacion';
import { Asistente, DESCRIPCION_MEDIO_ASISTENCIA, MedioAsistencia } from '../../models/asistencia';

const FILAS_CARGA = 4;

/** Asistentes de una reunión (presentacional), paginados en el servidor; en el móvil, tarjetas con el mismo paginador. */
@Component({
    selector: 'app-tabla-asistentes',
    imports: [DatePipe, PaginatorModule, SkeletonModule, TableModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './tabla-asistentes.html'
})
export class TablaAsistentesComponent {
    readonly asistentes = input.required<readonly Asistente[]>();
    readonly totalRegistros = input(0);
    readonly pagina = input(0);
    readonly tamanoPagina = input(FILAS_POR_PAGINA);
    readonly cargando = input(false);
    /** Hay un filtro por nombre: el vacío dice «nadie con ese nombre» en lugar de «nadie aún». */
    readonly filtrando = input(false);

    readonly cambiarPagina = output<PaginaPedida>();

    protected readonly opcionesPorPagina = OPCIONES_FILAS_POR_PAGINA;
    protected readonly plantillaReporte = PLANTILLA_REPORTE_PAGINA;
    protected readonly filasCarga = Array.from({ length: FILAS_CARGA }, (_, indice) => indice);
    protected readonly filas = computed(() => [...this.asistentes()]);
    protected readonly primerRegistro = computed(() => this.pagina() * this.tamanoPagina());

    protected medio(medio: MedioAsistencia) {
        return DESCRIPCION_MEDIO_ASISTENCIA[medio];
    }

    protected alPaginar(estado: PaginatorState): void {
        this.cambiarPagina.emit(paginaPedida(estado, this.tamanoPagina()));
    }
}
