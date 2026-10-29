import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { haceCuanto } from '../../models/seguimiento';
import { TendenciaAsistenciaComponent } from '../tendencia-asistencia/tendencia-asistencia';

/** Lo que hace falta para pintar los indicadores; lo cumple el resumen del miembro. */
export interface IndicadoresAsistencia {
    readonly porcentaje: number | null;
    readonly porcentajeAnterior: number | null;
    readonly asistidas: number;
    readonly reunionesQueCuentan: number;
    readonly faltasSeguidas: number;
    readonly ultimaAsistencia: string | null;
}

/** Indicadores de asistencia de un miembro (presentacional): porcentaje y tendencia, faltas seguidas y última vez. */
@Component({
    selector: 'app-indicadores-asistencia',
    imports: [TendenciaAsistenciaComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './indicadores-asistencia.html'
})
export class IndicadoresAsistenciaComponent {
    readonly indicadores = input.required<IndicadoresAsistencia>();

    protected readonly hace = haceCuanto;
}
