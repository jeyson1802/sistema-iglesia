import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { FILAS_POR_PAGINA, OPCIONES_FILAS_POR_PAGINA, PLANTILLA_REPORTE_PAGINA } from '@/app/shared/utils/paginacion';
import { MiembroReunion } from '../../models/asistencia';

/** Pedido de cambiar la presencia de un integrante. */
export interface CambioPresencia {
    readonly miembro: MiembroReunion;
    readonly presente: boolean;
}

/** Integrantes del grupo con un interruptor «Presente» cada uno y «Marcar a todos» (presentacional). */
@Component({
    selector: 'app-lista-integrantes',
    imports: [FormsModule, ButtonModule, SkeletonModule, TableModule, ToggleSwitchModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './lista-integrantes.html'
})
export class ListaIntegrantesComponent {
    readonly integrantes = input.required<readonly MiembroReunion[]>();
    readonly cargando = input(false);
    /** Integrantes cuyo cambio se está guardando. */
    readonly procesando = input<ReadonlySet<number>>(new Set());

    readonly cambiar = output<CambioPresencia>();
    readonly marcarTodos = output<void>();

    protected readonly filasPorPagina = FILAS_POR_PAGINA;
    protected readonly opcionesPorPagina = OPCIONES_FILAS_POR_PAGINA;
    protected readonly plantillaReporte = PLANTILLA_REPORTE_PAGINA;
    protected readonly filas = computed(() => [...this.integrantes()]);
    protected readonly presentes = computed(() => this.integrantes().filter(({ presente }) => presente).length);
    protected readonly todosPresentes = computed(() => this.integrantes().length > 0 && this.presentes() === this.integrantes().length);
}
