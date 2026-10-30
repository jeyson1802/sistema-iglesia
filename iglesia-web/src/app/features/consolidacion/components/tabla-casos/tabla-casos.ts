import { ChangeDetectionStrategy, Component, computed, input, model, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CheckboxModule } from 'primeng/checkbox';
import { PaginatorModule, PaginatorState } from 'primeng/paginator';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ETIQUETA_CONDICION } from '@/app/features/miembros/models/miembro';
import { FILAS_POR_PAGINA, OPCIONES_FILAS_POR_PAGINA, PaginaPedida, paginaPedida, PLANTILLA_REPORTE_PAGINA } from '@/app/shared/utils/paginacion';
import { CasoResumen, EstadoCaso, ETIQUETA_ESTADO_CASO, etiquetaResultado, fechaHoraCompleta, haceDias, iniciales, resumenContactos, SEVERIDAD_ESTADO_CASO } from '../../models/consolidacion';

const FILAS_CARGA = 5;

/**
 * Personas de una ronda (presentacional), paginadas en el servidor: persona, consolidador, estado, contactos por medio,
 * último contacto y resultado. Con `seleccionable`, casillas para las acciones masivas. En el móvil, tarjetas con el
 * mismo paginador. Un clic en la fila abre el caso.
 */
@Component({
    selector: 'app-tabla-casos',
    imports: [FormsModule, CheckboxModule, PaginatorModule, SkeletonModule, TableModule, TagModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './tabla-casos.html'
})
export class TablaCasosComponent {
    readonly casos = input.required<readonly CasoResumen[]>();
    readonly totalRegistros = input(0);
    readonly pagina = input(0);
    readonly tamanoPagina = input(FILAS_POR_PAGINA);
    readonly cargando = input(false);
    readonly seleccionable = input(false);
    readonly mensajeVacio = input('No hay personas con estos filtros.');
    readonly seleccion = model<CasoResumen[]>([]);

    readonly cambiarPagina = output<PaginaPedida>();
    readonly ver = output<CasoResumen>();

    protected readonly opcionesPorPagina = OPCIONES_FILAS_POR_PAGINA;
    protected readonly plantillaReporte = PLANTILLA_REPORTE_PAGINA;
    protected readonly filasCarga = Array.from({ length: FILAS_CARGA }, (_, indice) => indice);
    protected readonly filas = computed(() => [...this.casos()]);
    protected readonly primerRegistro = computed(() => this.pagina() * this.tamanoPagina());
    protected readonly idsSeleccionados = computed(() => new Set(this.seleccion().map(({ id }) => id)));
    protected readonly etiquetaNuevo = ETIQUETA_CONDICION.NUEVO;
    protected readonly iniciales = iniciales;
    protected readonly resumen = resumenContactos;
    protected readonly hace = haceDias;
    protected readonly completa = fechaHoraCompleta;

    protected estado(estado: EstadoCaso): string {
        return ETIQUETA_ESTADO_CASO[estado];
    }

    protected severidad(estado: EstadoCaso) {
        return SEVERIDAD_ESTADO_CASO[estado];
    }

    protected ultimo(caso: CasoResumen): string | null {
        return caso.ultimoContacto ? etiquetaResultado(caso.ultimoContacto.resultado, caso.ultimoContacto.medio) : null;
    }

    protected alternar(caso: CasoResumen, marcado: boolean): void {
        this.seleccion.update((seleccion) => (marcado ? [...seleccion, caso] : seleccion.filter(({ id }) => id !== caso.id)));
    }

    protected alPaginar(estado: PaginatorState): void {
        this.cambiarPagina.emit(paginaPedida(estado, this.tamanoPagina()));
    }
}
