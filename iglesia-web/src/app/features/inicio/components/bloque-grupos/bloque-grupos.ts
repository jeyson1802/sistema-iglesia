import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { TableModule } from 'primeng/table';
import { FILAS_POR_PAGINA, OPCIONES_FILAS_POR_PAGINA, PLANTILLA_REPORTE_PAGINA } from '@/app/shared/utils/paginacion';
import { EstadisticasGrupos, formatoNumero, porcentajeDe } from '../../models/estadisticas';
import { BloqueEstadisticoComponent } from '../bloque-estadistico/bloque-estadistico';
import { TarjetaIndicadorComponent } from '../tarjeta-indicador/tarjeta-indicador';

/**
 * Bloque «Grupos» (presentacional): grupos activos, integrantes promedio, parte de los miembros que está en un
 * grupo, grupos sin reuniones y asistencia de los integrantes; abajo, los grupos de mejor a menor asistencia
 * (paginados en el navegador).
 */
@Component({
    selector: 'app-bloque-grupos',
    imports: [TableModule, BloqueEstadisticoComponent, TarjetaIndicadorComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './bloque-grupos.html'
})
export class BloqueGruposComponent {
    readonly datos = input<EstadisticasGrupos | null>(null);
    readonly cargando = input(false);
    readonly error = input(false);
    readonly titulo = input('Grupos');
    readonly ruta = input<string | null>('/grupos');
    readonly reintentar = output<void>();

    protected readonly numero = formatoNumero;
    protected readonly filas = computed(() => [...(this.datos()?.porGrupo ?? [])]);
    protected readonly porcentajeDe = porcentajeDe;
    protected readonly filasPorPagina = FILAS_POR_PAGINA;
    protected readonly opcionesPorPagina = OPCIONES_FILAS_POR_PAGINA;
    protected readonly plantillaReporte = PLANTILLA_REPORTE_PAGINA;

    protected porcentaje(valor: number | null): string {
        return valor === null ? '—' : `${valor} %`;
    }
}
