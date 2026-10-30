import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { PaginatorModule, PaginatorState } from 'primeng/paginator';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { FILAS_POR_PAGINA, OPCIONES_FILAS_POR_PAGINA, PaginaPedida, paginaPedida, PLANTILLA_REPORTE_PAGINA } from '@/app/shared/utils/paginacion';
import { fechaConDia } from '@/app/shared/utils/fecha';
import { ETIQUETA_ESTADO_RONDA, EstadoRonda, ICONO_ESTADO_RONDA, porcentaje, Ronda, SEVERIDAD_ESTADO_RONDA } from '../../models/consolidacion';
import { MedidorRondaComponent } from '../medidor-ronda/medidor-ronda';

const FILAS_CARGA = 4;

/**
 * Todas las rondas (presentacional), paginadas en el servidor: nombre, fechas, estado, personas y avance. En el móvil,
 * tarjetas con el mismo paginador. Un clic abre la ronda.
 */
@Component({
    selector: 'app-tabla-rondas',
    imports: [PaginatorModule, SkeletonModule, TableModule, TagModule, MedidorRondaComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './tabla-rondas.html'
})
export class TablaRondasComponent {
    readonly rondas = input.required<readonly Ronda[]>();
    readonly totalRegistros = input(0);
    readonly pagina = input(0);
    readonly tamanoPagina = input(FILAS_POR_PAGINA);
    readonly cargando = input(false);
    readonly mensajeVacio = input('No hay rondas con estos filtros.');

    readonly cambiarPagina = output<PaginaPedida>();
    readonly ver = output<Ronda>();

    protected readonly opcionesPorPagina = OPCIONES_FILAS_POR_PAGINA;
    protected readonly plantillaReporte = PLANTILLA_REPORTE_PAGINA;
    protected readonly filasCarga = Array.from({ length: FILAS_CARGA }, (_, indice) => indice);
    protected readonly filas = computed(() => [...this.rondas()]);
    protected readonly primerRegistro = computed(() => this.pagina() * this.tamanoPagina());
    protected readonly conDia = fechaConDia;

    protected etiqueta(estado: EstadoRonda): string {
        return ETIQUETA_ESTADO_RONDA[estado];
    }

    protected severidad(estado: EstadoRonda) {
        return SEVERIDAD_ESTADO_RONDA[estado];
    }

    protected icono(estado: EstadoRonda): string {
        return ICONO_ESTADO_RONDA[estado];
    }

    protected avance(ronda: Ronda): number {
        return porcentaje(ronda.cerrados, ronda.personas);
    }

    protected alPaginar(estado: PaginatorState): void {
        this.cambiarPagina.emit(paginaPedida(estado, this.tamanoPagina()));
    }
}
