import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { AsistenciaMiembro, ETIQUETA_TIPO_REUNION, SEVERIDAD_TIPO_REUNION, TipoReunion } from '@/app/features/asistencia/models/asistencia';
import { IndicadoresAsistenciaComponent } from '@/app/features/asistencia/components/indicadores-asistencia/indicadores-asistencia';
import { SemanasAsistenciaComponent } from '@/app/features/asistencia/components/semanas-asistencia/semanas-asistencia';
import { fechaConDia } from '@/app/shared/utils/fecha';
import { FILAS_POR_PAGINA, OPCIONES_FILAS_POR_PAGINA, PLANTILLA_REPORTE_PAGINA } from '@/app/shared/utils/paginacion';

/**
 * «Mi asistencia» (presentacional): porcentaje de las últimas 8 semanas con su tendencia, faltas seguidas y última
 * vez, las últimas 12 semanas y sus últimas reuniones (paginadas en el navegador).
 */
@Component({
    selector: 'app-mi-asistencia',
    imports: [ButtonModule, SkeletonModule, TableModule, TagModule, IndicadoresAsistenciaComponent, SemanasAsistenciaComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './mi-asistencia.html',
    host: { class: 'block' }
})
export class MiAsistenciaComponent {
    readonly asistencia = input<AsistenciaMiembro | null>(null);
    readonly cargando = input(false);
    readonly error = input(false);
    readonly reintentar = output<void>();

    protected readonly conDia = fechaConDia;
    protected readonly ultimas = computed(() => [...(this.asistencia()?.ultimas ?? [])]);
    protected readonly filasPorPagina = FILAS_POR_PAGINA;
    protected readonly opcionesPorPagina = OPCIONES_FILAS_POR_PAGINA;
    protected readonly plantillaReporte = PLANTILLA_REPORTE_PAGINA;

    protected tipo(tipo: TipoReunion): string {
        return ETIQUETA_TIPO_REUNION[tipo];
    }

    protected severidad(tipo: TipoReunion) {
        return SEVERIDAD_TIPO_REUNION[tipo];
    }
}
