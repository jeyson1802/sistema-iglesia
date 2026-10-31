import { NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { PaginatorModule, PaginatorState } from 'primeng/paginator';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { FILAS_POR_PAGINA, OPCIONES_FILAS_POR_PAGINA, PaginaPedida, paginaPedida, PLANTILLA_REPORTE_PAGINA } from '@/app/shared/utils/paginacion';
import { haceCuanto, PersonaSeguimiento } from '../../models/seguimiento';
import { EtiquetasAlertaComponent } from '../etiquetas-alerta/etiquetas-alerta';
import { TendenciaAsistenciaComponent } from '../tendencia-asistencia/tendencia-asistencia';

const FILAS_CARGA = 5;

/**
 * Personas en seguimiento (presentacional): nombre, grupo y líder, porcentaje con su tendencia, faltas seguidas,
 * última vez y alertas. Paginada en el servidor si `paginaEnServidor`; si no, en el navegador (p. ej. los integrantes
 * de un grupo). En el móvil, tarjetas con el mismo paginador. Las filas abren el detalle solo si `navegable`.
 */
@Component({
    selector: 'app-tabla-seguimiento',
    imports: [NgTemplateOutlet, PaginatorModule, SkeletonModule, TableModule, EtiquetasAlertaComponent, TendenciaAsistenciaComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './tabla-seguimiento.html'
})
export class TablaSeguimientoComponent {
    readonly personas = input.required<readonly PersonaSeguimiento[]>();
    readonly totalRegistros = input(0);
    readonly pagina = input(0);
    readonly tamanoPagina = input(FILAS_POR_PAGINA);
    readonly cargando = input(false);
    readonly paginaEnServidor = input(true);
    /** Oculta la columna de grupo (p. ej. dentro del propio grupo). */
    readonly mostrarGrupo = input(true);
    readonly mensajeVacio = input('Nadie necesita seguimiento con estos filtros.');
    /** Si las filas abren el detalle de la persona; sin permiso para verlo, se muestran sin enlace. */
    readonly navegable = input(true);

    readonly cambiarPagina = output<PaginaPedida>();
    readonly ver = output<PersonaSeguimiento>();

    protected readonly opcionesPorPagina = OPCIONES_FILAS_POR_PAGINA;
    protected readonly plantillaReporte = PLANTILLA_REPORTE_PAGINA;
    protected readonly filasCarga = Array.from({ length: FILAS_CARGA }, (_, indice) => indice);
    /** En el navegador, la página local que se muestra; en el servidor, la que llegó. */
    protected readonly paginaLocal = computed(() => {
        const personas = this.personas();
        if (this.paginaEnServidor()) {
            return personas;
        }
        const inicio = this.pagina() * this.tamanoPagina();
        return personas.slice(inicio, inicio + this.tamanoPagina());
    });
    protected readonly filas = computed(() => [...this.paginaLocal()]);
    protected readonly total = computed(() => (this.paginaEnServidor() ? this.totalRegistros() : this.personas().length));
    protected readonly primerRegistro = computed(() => this.pagina() * this.tamanoPagina());
    protected readonly hace = haceCuanto;

    protected alPaginar(estado: PaginatorState): void {
        this.cambiarPagina.emit(paginaPedida(estado, this.tamanoPagina()));
    }
}
