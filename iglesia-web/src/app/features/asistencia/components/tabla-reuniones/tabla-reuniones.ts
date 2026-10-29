import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { PaginatorModule, PaginatorState } from 'primeng/paginator';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { fechaConDia } from '@/app/shared/utils/fecha';
import { FILAS_POR_PAGINA, OPCIONES_FILAS_POR_PAGINA, PaginaPedida, paginaPedida, PLANTILLA_REPORTE_PAGINA } from '@/app/shared/utils/paginacion';
import { ETIQUETA_TIPO_REUNION, horaCorta, Reunion, SEVERIDAD_TIPO_REUNION, TipoReunion } from '../../models/asistencia';

const FILAS_CARGA = 4;

/** Historial de reuniones (presentacional), paginado en el servidor; en el móvil, tarjetas con el mismo paginador. */
@Component({
    selector: 'app-tabla-reuniones',
    imports: [DatePipe, PaginatorModule, SkeletonModule, TableModule, TagModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './tabla-reuniones.html'
})
export class TablaReunionesComponent {
    readonly reuniones = input.required<readonly Reunion[]>();
    readonly totalRegistros = input(0);
    readonly pagina = input(0);
    readonly tamanoPagina = input(FILAS_POR_PAGINA);
    readonly cargando = input(false);

    readonly cambiarPagina = output<PaginaPedida>();
    readonly ver = output<Reunion>();

    protected readonly opcionesPorPagina = OPCIONES_FILAS_POR_PAGINA;
    protected readonly plantillaReporte = PLANTILLA_REPORTE_PAGINA;
    protected readonly filasCarga = Array.from({ length: FILAS_CARGA }, (_, indice) => indice);
    protected readonly filas = computed(() => [...this.reuniones()]);
    protected readonly primerRegistro = computed(() => this.pagina() * this.tamanoPagina());
    protected readonly hora = horaCorta;
    protected readonly conDia = fechaConDia;

    protected tipo(tipo: TipoReunion): string {
        return ETIQUETA_TIPO_REUNION[tipo];
    }

    protected severidad(tipo: TipoReunion) {
        return SEVERIDAD_TIPO_REUNION[tipo];
    }

    protected alPaginar(estado: PaginatorState): void {
        this.cambiarPagina.emit(paginaPedida(estado, this.tamanoPagina()));
    }
}
