import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { TagModule } from 'primeng/tag';
import { CAMPO_CONTEO, DESCRIPCION_ALERTA, TIPOS_ALERTA } from '@/app/features/asistencia/models/seguimiento';
import { GraficoEvolucionComponent, SerieGrafico } from '@/app/shared/components/grafico-evolucion/grafico-evolucion';
import { EstadisticasAsistencia, etiquetaEje, etiquetaPunto, formatoNumero } from '../../models/estadisticas';
import { BloqueEstadisticoComponent } from '../bloque-estadistico/bloque-estadistico';
import { TarjetaIndicadorComponent } from '../tarjeta-indicador/tarjeta-indicador';

/**
 * Bloque «Asistencia» (presentacional): asistentes promedio por día de culto, cultos, personas que asistieron y
 * nuevos que volvieron, frente al periodo anterior; la serie de asistentes y las personas con alerta por tipo.
 */
@Component({
    selector: 'app-bloque-asistencia',
    imports: [TagModule, BloqueEstadisticoComponent, TarjetaIndicadorComponent, GraficoEvolucionComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    templateUrl: './bloque-asistencia.html'
})
export class BloqueAsistenciaComponent {
    readonly datos = input<EstadisticasAsistencia | null>(null);
    readonly cargando = input(false);
    readonly error = input(false);
    readonly titulo = input('Asistencia');
    readonly ruta = input<string | null>('/asistencia/seguimiento');
    readonly reintentar = output<void>();

    protected readonly numero = formatoNumero;
    protected readonly eje = etiquetaEje;
    protected readonly etiquetas = computed(() => {
        const datos = this.datos();
        return datos ? datos.serie.map(({ inicio }) => etiquetaPunto(inicio, datos.periodo.granularidad)) : [];
    });
    protected readonly series = computed<readonly SerieGrafico[]>(() => [{ etiqueta: 'Asistentes promedio por culto', valores: (this.datos()?.serie ?? []).map(({ asistentesPromedio }) => asistentesPromedio), tono: 'primario' }]);
    protected readonly huboCultos = computed(() => (this.datos()?.serie ?? []).some(({ cultos }) => cultos > 0));
    protected readonly alertas = computed(() => {
        const conteos = this.datos()?.alertas;
        return conteos ? TIPOS_ALERTA.map((tipo) => ({ tipo, ...DESCRIPCION_ALERTA[tipo], cantidad: conteos[CAMPO_CONTEO[tipo]] })) : [];
    });
}
