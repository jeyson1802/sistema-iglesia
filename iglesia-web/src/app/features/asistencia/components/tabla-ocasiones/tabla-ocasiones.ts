import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { PaginatorModule, PaginatorState } from 'primeng/paginator';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { fechaConDia } from '@/app/shared/utils/fecha';
import { FILAS_POR_PAGINA, OPCIONES_FILAS_POR_PAGINA, PaginaPedida, paginaPedida, PLANTILLA_REPORTE_PAGINA } from '@/app/shared/utils/paginacion';
import { horaCorta } from '../../models/asistencia';
import { DESCRIPCION_ESTADO_OCASION, Ocasion } from '../../models/seguimiento';

const FILAS_CARGA = 4;

/** Qué es cada ocasión según su tipo: un día de culto agrupa los cultos de esa fecha. */
const ETIQUETA_OCASION = { CULTO: 'Día de culto', GRUPO: 'Grupo', EVENTO: 'Evento' } as const;

/**
 * Historial de ocasiones de un miembro (presentacional), paginado en el servidor: fecha, ocasión, si asistió o faltó y
 * a qué reunión fue. En el móvil, tarjetas con el mismo paginador.
 */
@Component({
    selector: 'app-tabla-ocasiones',
    imports: [PaginatorModule, SkeletonModule, TableModule, TagModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './tabla-ocasiones.html'
})
export class TablaOcasionesComponent {
    readonly ocasiones = input.required<readonly Ocasion[]>();
    readonly totalRegistros = input(0);
    readonly pagina = input(0);
    readonly tamanoPagina = input(FILAS_POR_PAGINA);
    readonly cargando = input(false);

    readonly cambiarPagina = output<PaginaPedida>();

    protected readonly opcionesPorPagina = OPCIONES_FILAS_POR_PAGINA;
    protected readonly plantillaReporte = PLANTILLA_REPORTE_PAGINA;
    protected readonly filasCarga = Array.from({ length: FILAS_CARGA }, (_, indice) => indice);
    protected readonly filas = computed(() => [...this.ocasiones()]);
    protected readonly primerRegistro = computed(() => this.pagina() * this.tamanoPagina());
    protected readonly conDia = fechaConDia;
    protected readonly hora = horaCorta;

    protected estadoDe({ estado }: Ocasion) {
        return DESCRIPCION_ESTADO_OCASION[estado];
    }

    protected tipoDe({ tipo }: Ocasion): string {
        return ETIQUETA_OCASION[tipo];
    }

    protected alPaginar(estado: PaginatorState): void {
        this.cambiarPagina.emit(paginaPedida(estado, this.tamanoPagina()));
    }
}
