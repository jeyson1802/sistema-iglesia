import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { DiaSemana, ETIQUETA_DIA } from '@/app/core/models/dia-semana';
import { FILAS_POR_PAGINA, OPCIONES_FILAS_POR_PAGINA, PLANTILLA_REPORTE_PAGINA } from '@/app/shared/utils/paginacion';
import { CultoProgramado, horaCorta } from '../../models/asistencia';

const FILAS_CARGA = 3;

/** Grilla de horarios de culto (presentacional), paginada en el navegador: la lista llega completa. */
@Component({
    selector: 'app-tabla-cultos',
    imports: [ButtonModule, SkeletonModule, TableModule, TagModule, TooltipModule],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './tabla-cultos.html'
})
export class TablaCultosComponent {
    readonly cultos = input.required<readonly CultoProgramado[]>();
    readonly cargando = input(false);
    /** Id del culto cuyo estado se está cambiando, para deshabilitar sus acciones. */
    readonly procesando = input<number | null>(null);

    readonly editar = output<CultoProgramado>();
    readonly cambiarEstado = output<CultoProgramado>();

    protected readonly filasPorPagina = FILAS_POR_PAGINA;
    protected readonly opcionesPorPagina = OPCIONES_FILAS_POR_PAGINA;
    protected readonly plantillaReporte = PLANTILLA_REPORTE_PAGINA;
    protected readonly filasCarga = Array.from({ length: FILAS_CARGA }, (_, indice) => indice);
    protected readonly filas = computed(() => [...this.cultos()]);
    protected readonly hora = horaCorta;

    protected etiquetaDia(dia: DiaSemana): string {
        return ETIQUETA_DIA[dia];
    }
}
