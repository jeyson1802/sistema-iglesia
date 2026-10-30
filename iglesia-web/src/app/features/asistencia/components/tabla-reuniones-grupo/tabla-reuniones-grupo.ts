import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { PaginatorModule, PaginatorState } from 'primeng/paginator';
import { ProgressBarModule } from 'primeng/progressbar';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { fechaConDia, hoyIso } from '@/app/shared/utils/fecha';
import { FILAS_POR_PAGINA, OPCIONES_FILAS_POR_PAGINA, PaginaPedida, paginaPedida, PLANTILLA_REPORTE_PAGINA } from '@/app/shared/utils/paginacion';
import { horaCorta } from '../../models/asistencia';
import { ReunionGrupo } from '../../models/asistencia-grupo';

const FILAS_CARGA = 4;

/**
 * Reuniones de un grupo por día (presentacional), paginadas en el servidor: cuántos integrantes asistieron de los
 * esperados, cuántos faltaron y cuántos visitantes hubo. En el móvil, tarjetas con el mismo paginador.
 */
@Component({
    selector: 'app-tabla-reuniones-grupo',
    imports: [PaginatorModule, ProgressBarModule, SkeletonModule, TableModule, TagModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './tabla-reuniones-grupo.html'
})
export class TablaReunionesGrupoComponent {
    readonly reuniones = input.required<readonly ReunionGrupo[]>();
    readonly totalRegistros = input(0);
    readonly pagina = input(0);
    readonly tamanoPagina = input(FILAS_POR_PAGINA);
    readonly cargando = input(false);

    readonly cambiarPagina = output<PaginaPedida>();
    readonly ver = output<ReunionGrupo>();

    protected readonly opcionesPorPagina = OPCIONES_FILAS_POR_PAGINA;
    protected readonly plantillaReporte = PLANTILLA_REPORTE_PAGINA;
    protected readonly filasCarga = Array.from({ length: FILAS_CARGA }, (_, indice) => indice);
    protected readonly filas = computed(() => [...this.reuniones()]);
    protected readonly primerRegistro = computed(() => this.pagina() * this.tamanoPagina());
    protected readonly hoy = hoyIso();
    protected readonly conDia = fechaConDia;
    protected readonly hora = horaCorta;

    protected alPaginar(estado: PaginatorState): void {
        this.cambiarPagina.emit(paginaPedida(estado, this.tamanoPagina()));
    }

    /** Texto para lectores de pantalla y para el título de la fila. */
    protected descripcion(reunion: ReunionGrupo): string {
        return `Ver quiénes asistieron el ${this.conDia(reunion.fecha)}: ${reunion.asistieron} de ${reunion.esperados}`;
    }
}
